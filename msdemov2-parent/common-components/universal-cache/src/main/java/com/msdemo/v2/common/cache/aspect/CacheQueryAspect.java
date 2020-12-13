package com.msdemo.v2.common.cache.aspect;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.msdemo.v2.common.cache.config.CacheEnvHolder;
import com.msdemo.v2.common.cache.core.CachedQuery;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.core.IUniversalCache;

/**
 * @annotation() 表示标注了指定注解的目标类方法 target() 通过类名指定，同时包含所有子类, @target() 所有标注了指定注解的类
 *               within() 通过类名指定切点, @within() 匹配标注了指定注解的类及其所有子类 args()
 *               通过目标类方法的参数类型指定切点, @args() 通过目标类参数的对象类型是否标注了指定注解指定切点
 * @Order(1) //切面先执行Order最小的切面
 */

@SuppressWarnings("unchecked")
@Aspect
public class CacheQueryAspect{

	private static final Logger logger = LoggerFactory.getLogger(CacheQueryAspect.class);

	@Autowired
	CacheEnvHolder holder;

	/*
	 * 从cache中获取查询结果 
	 */
	@Around("target(cachedService) && @annotation(cachedQuery) "
//			+ "&& (execution(public * selectBy*(..)) || execution(public * findBy*(..)))"
			)
	public Object cachedQuery(ProceedingJoinPoint pjd, IUniversalCache<?> cachedService, CachedQuery cachedQuery)
			throws Throwable {
		String cacheKey = CacheEnvHolder.getCacheKey(cachedService);
		if (!holder.isCacheKeyEnabled(cacheKey)) {
			return pjd.proceed(); // skip cached query to invoke native method
		}
				
		if (!holder.getStrategy(cacheKey).containsKey(cacheKey)) {
			logger.warn("{}: no available cache, reload all...",cacheKey);
			List<?> allRecords = (List<?>) cachedService.selectAll();
			holder.getStrategy(cacheKey).putAll(cacheKey, (List<Object>) allRecords);
		}

		Object result = holder.getStrategy(cacheKey).get(cacheKey, cachedQuery, pjd);
		return result;
	}

	/**
	 * 查询全表数据，并缓存到缓存策略实现类中
	 */
	@Around("target(cachedTable) && execution(public * selectAll(..)) ")
	public Object cachedQueryAll(ProceedingJoinPoint pjd, IUniversalCache<?> cachedTable) throws Throwable {
		Object result = null;

		String cacheKey = CacheEnvHolder.getCacheKey(cachedTable);
		if (holder.getStrategy(cacheKey).containsKey(cacheKey)) {
			result = holder.getStrategy(cacheKey).getAll(cacheKey);
		} else {
			result = pjd.proceed();
			ICacheStoreStrategy strategy=holder.getStrategy(cacheKey);
			if (strategy !=null){
				strategy.putAll(cacheKey, (List<Object>) result);
			}
			logger.debug("load {} record(s) by {} with native method", ((List<Object>) result).size(), cacheKey);
		}

		return result;
	}
	
}
