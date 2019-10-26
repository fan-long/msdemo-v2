package com.msdemo.v2.common.cache.core;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
		Object result = null;
		String cacheKey = holder.getCacheKey(cachedTable);
		MethodSignature ms = (MethodSignature) pjd.getSignature();
		boolean isList = ms.getMethod().getReturnType().isArray();

		if (!holder.isCacheKeyEnabled(cacheKey)) {
			return pjd.proceed(); // skip to invoke real method
		}
		// TODO: 启动获取所有@CachedQuery类方法，并缓存方法参数名，参考CacheTest
		// ms.getMethod()可以获取method对象，如果instanceof
		// ParameterizedType，通过getActualTypeArguments可获取实际类型
		String groupFields = String.join(ICacheStoreStrategy.KEY_DELIMITER, cachedQuery.value());
		if (!holder.getStrategy(cacheKey).hasCache(cacheKey)) {
			List<?> allRecords = (List<?>) cachedTable.selectAll();
			holder.getStrategy(cacheKey).putAll(cacheKey, (List<Object>) allRecords);
		}

		result = holder.getStrategy(cacheKey).get(cacheKey, groupFields, isList, cachedQuery.cloned(), pjd);
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
			holder.getStrategy(cacheKey).putAll(cacheKey, (List<Object>) result);
			logger.info("[from DB] load {} records by {} ", ((List<Object>) result).size(), cacheKey);
		}

		return result;
	}
	
}
