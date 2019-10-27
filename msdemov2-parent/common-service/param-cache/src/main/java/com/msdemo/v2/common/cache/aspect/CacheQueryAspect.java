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
import com.msdemo.v2.common.cache.core.ICachedParamTable;

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
	@Around("target(cachedTable) && @annotation(cachedQuery) && execution(public * selectBy*(..)) ")
	public Object cachedQuery(ProceedingJoinPoint pjd, ICachedParamTable<?> cachedTable, CachedQuery cachedQuery)
			throws Throwable {
		String cacheKey = holder.getCacheKey(cachedTable);
		if (!holder.isCacheKeyEnabled(cacheKey)) {
			return pjd.proceed(); // skip to invoke native method
		}
		
		// TODO: 启动获取所有@CachedQuery类方法，并缓存方法参数名，参考CacheTest
		// ms.getMethod()可以获取method对象，如果instanceof
		// ParameterizedType，通过getActualTypeArguments可获取实际类型
		//String groupFields = String.join(ICacheStoreStrategy.KEY_DELIMITER, cachedQuery.value());
		
		if (!holder.getStrategy(cacheKey).hasCache(cacheKey)) {
			logger.warn("{}: no available cache, reload all...",cacheKey);
			List<?> allRecords = (List<?>) cachedTable.selectAll();
			holder.getStrategy(cacheKey).putAll(cacheKey, (List<Object>) allRecords);
		}

		Object result = holder.getStrategy(cacheKey).get(cacheKey, cachedQuery, pjd);
		return result;
	}

	/**
	 * 查询全表数据，并缓存到缓存策略实现类中
	 */
	@Around("target(cachedTable) && execution(public * selectAll(..)) ")
	public Object cachedQueryAll(ProceedingJoinPoint pjd, ICachedParamTable<?> cachedTable) throws Throwable {
		Object result = null;

		String cacheKey = holder.getCacheKey(cachedTable);
		if (holder.getStrategy(cacheKey).hasCache(cacheKey)) {
			result = holder.getStrategy(cacheKey).getAll(cacheKey);
		} else {
			result = pjd.proceed();
			ICacheStoreStrategy strategy=holder.getStrategy(cacheKey);
			if (strategy !=null){
				strategy.putAll(cacheKey, (List<Object>) result);
			}
			logger.debug("load {} record(s) by {} by native method", ((List<Object>) result).size(), cacheKey);
		}

		return result;
	}
	
}
