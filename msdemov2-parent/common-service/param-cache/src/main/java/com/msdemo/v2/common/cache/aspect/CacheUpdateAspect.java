package com.msdemo.v2.common.cache.aspect;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.cache.config.CacheEnvHolder;
import com.msdemo.v2.common.cache.core.CacheSyncDTO;
import com.msdemo.v2.common.cache.core.CacheSyncDTO.Action;
import com.msdemo.v2.common.cache.core.ICacheSyncPublisher;
import com.msdemo.v2.common.cache.core.ICachedParamTable;

@Aspect
public class CacheUpdateAspect{

	@Autowired
	CacheEnvHolder holder;

	@Autowired
	ICacheSyncPublisher publisher;

//	private static final Logger logger = LoggerFactory.getLogger(CacheUpdateAspect.class);
	
	/**
	 * 记录对缓存表的数据维护操作，事务提交后发布缓存刷新通知
	 * 
	 * @param cachedTable
	 */

	@Around("target(cachedTable) && (execution(public * update*(..)) || execution(public * insert*(..)) || execution(public * delete*(..))) ")
	public Object afterModification(ProceedingJoinPoint pjd,ICachedParamTable<?> cachedTable) throws Throwable{
		Object result=pjd.proceed();
		String cacheKey = holder.getCacheKey(cachedTable);
		CacheSyncDTO dto =new CacheSyncDTO();
		dto.setCacheKey(cacheKey);
		dto.setAction(Action.fromMethod(pjd.getSignature().getName()));
		dto.setValue(pjd.getArgs());
		publisher.recordModification(dto);
		return result;
	}

	// FIXME: check if this method invocation nested in a existing transaction,
	// should only publish after existing transaction committed
	@Around("@annotation(transaction)")
	public Object doTransaction(ProceedingJoinPoint pjd, Transactional transaction) throws Throwable {
		if (transaction.propagation().equals(Propagation.REQUIRED)
				|| transaction.propagation().equals(Propagation.REQUIRES_NEW)) {
			publisher.initModification();
			//do transaction
			Object result = pjd.proceed();
			//then refresh cache
			if (!ICacheSyncPublisher.modifiedMapperList.get().isEmpty())
				refreshCache(ICacheSyncPublisher.modifiedMapperList.get());
			return result;
		} else
			return pjd.proceed();
	}

	//TODO: it's better to refresh async, but modifiedMapperList can not be accessed in async thread
	private void refreshCache(List<CacheSyncDTO> modifiedCaches){
		ArrayList<CacheSyncDTO> publishList = new ArrayList<>();
		for (CacheSyncDTO cache : modifiedCaches) {
			if (holder.isCacheKeyEnabled(cache.getCacheKey())){ 
				if (holder.getStrategy(cache.getCacheKey()).isPublishNeeded()) 
					// local-copied cache
					publishList.add(cache);
				else {
					// middle-ware cache
					// FIXME: async refresh
					holder.getStrategy(cache.getCacheKey()).refresh(cache.getCacheKey());
				}
			}
		}
		publisher.publishModification(publishList);
	}
	
}
