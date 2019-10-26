package com.msdemo.v2.common.cache.aspect;

import java.util.ArrayList;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.cache.config.CacheEnvHolder;
import com.msdemo.v2.common.cache.core.ICacheSyncStrategy;
import com.msdemo.v2.common.cache.core.ICachedParamTable;

@Aspect
public class CacheUpdateAspect{

	@Autowired
	CacheEnvHolder holder;

	@Autowired
	ICacheSyncStrategy syncAdapter;

	
	/**
	 * 记录对缓存表的数据维护操作，事务提交后发布缓存刷新通知
	 * 
	 * @param cachedTable
	 */

	@After("target(cachedTable) && (execution(public * update*(..)) || execution(public * insert*(..)) || execution(public * delete*(..))) ")
	public void afterModification(ICachedParamTable<?> cachedTable) {
		String cacheKey = holder.getCacheKey(cachedTable);
		syncAdapter.recordModification(cacheKey);
	}

	// FIXME: check if this method invocation nested in a existing transaction,
	// should only
	// publish after existing transaction committed
	@Around("@annotation(transaction)")
	public Object doTransaction(ProceedingJoinPoint pjd, Transactional transaction) throws Throwable {
		if (transaction.propagation().equals(Propagation.REQUIRED)
				|| transaction.propagation().equals(Propagation.REQUIRES_NEW)) {
			syncAdapter.initModification();
			Object result = pjd.proceed();
			ArrayList<String> publishList = new ArrayList<>();
			for (String cacheKey : ICacheSyncStrategy.modifiedMapperList.get()) {
				if (holder.getStrategy(cacheKey).isPublishNeeded()) // distribution
					publishList.add(cacheKey);
				else {
					// cache middle-ware
					// FIXME: async refresh
					holder.getStrategy(cacheKey).refresh(cacheKey);
				}
			}
			syncAdapter.publishModification(publishList);

			// ParamCacheConstants.modifiedMapperList.get().toArray(new
			// String[0])

			return result;

		} else
			return pjd.proceed();
	}

	
}
