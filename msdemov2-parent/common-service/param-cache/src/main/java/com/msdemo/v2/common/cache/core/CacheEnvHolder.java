package com.msdemo.v2.common.cache.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

public class CacheEnvHolder implements InitializingBean {
	
	private static Logger logger = LoggerFactory.getLogger(CacheEnvHolder.class);
	
	private static Map<String, ICacheStoreStrategy> CachekeyStrategyMapping 
		= new HashMap<>();

	private static HashMap<String, String> ClassCachekeyMapping
		= new HashMap<>();
	

	@Autowired
	ParamConfig paramConfig;
	
	@Autowired
	@Nullable
	ICachedParamTable<?>[] cachedMappers;

	@Autowired
	ICacheStoreStrategy[] cacheStrategy;
	
	@Override
	public void afterPropertiesSet() {
		HashMap<String,String> cacheTypeMapping=paramConfig.getType();
		if (cachedMappers == null || cacheTypeMapping == null)
			return;
		for (ICachedParamTable<?> cachedTable : cachedMappers) {
			String cacheKey=getCacheKey(cachedTable);
			String cacheType = cacheTypeMapping.get(cacheKey);
			for (ICacheStoreStrategy strategy : cacheStrategy) {
				if (StringUtils.equalsIgnoreCase(strategy.cacheType(), cacheType)){
					CachekeyStrategyMapping.put(cacheKey, strategy);
					//启动时初始化加载缓存
					strategy.refresh(cacheKey);
				}
			}
		}
		paramConfig.linkToHolder(this);
	}

	public boolean isCacheKeyEnabled(String cacheKey){
		return CachekeyStrategyMapping.containsKey(cacheKey);
	}
	
	public ICacheStoreStrategy getStrategy(String cacheKey){
		return CachekeyStrategyMapping.get(cacheKey);
	} 
	
	boolean changeCacheType(String cacheKey,String cacheType){
		
		for (ICacheStoreStrategy strategy : cacheStrategy) {
			if (StringUtils.equalsIgnoreCase(strategy.cacheType(), cacheType)){
				if (!CachekeyStrategyMapping.get(cacheKey).equals(strategy)){
					
					//clear cache of old strategy. 
					//Notes: Redis cache would be cleared more than 1 time by all nodes 
					CachekeyStrategyMapping.get(cacheKey).clear(cacheKey);
					//enable new strategy
					logger.info("replaced cache type of {} from {} to {}",cacheKey,
							CachekeyStrategyMapping.get(cacheKey).cacheType(),
							strategy.cacheType());
					//FIXME: empty cached data during refresh
					// consider to allow direct access from DB during refresh period?
					CachekeyStrategyMapping.put(cacheKey, strategy);
					//load cache with new strategy
					strategy.refresh(cacheKey);
					return true;
				}
			}
		}
		logger.warn("failed to replace cache type of {} to {}",cacheKey,
							CachekeyStrategyMapping.get(cacheKey).cacheType());
		return false;
	}
	void disableCache(String cacheKey){
		if (CachekeyStrategyMapping.containsKey(cacheKey)){
			CachekeyStrategyMapping.get(cacheKey).clear(cacheKey);
			logger.info("disable cache of {} with type: {}", cacheKey,CachekeyStrategyMapping.get(cacheKey).cacheType());
		}
	}
	boolean enableCache(String cacheKey,String cacheType){
		for (ICachedParamTable<?> cachedTable : cachedMappers) {
			//verify new cachekey  
			if(StringUtils.equals(cacheKey,getCacheKey(cachedTable))){
				for (ICacheStoreStrategy strategy : cacheStrategy) {
					//verify cache type
					if (StringUtils.equalsIgnoreCase(strategy.cacheType(), cacheType)){
						//enable and load cache
						CachekeyStrategyMapping.put(cacheKey, strategy);
						strategy.refresh(cacheKey);
						logger.info("enable cache of {} with type: {}", cacheKey,cacheType);
					}
				}
				return true;
			}
		}
		return false;
	}
	//use simple class name to identify cache key
	String getCacheKey(ICachedParamTable<?> cachedTable) {
		String className=cachedTable.toString();
		if (ClassCachekeyMapping.containsKey(className)) {
			return ClassCachekeyMapping.get(className);
		} else {
			// AopUtils.isAopProxy(cachedTable)
			if (AopUtils.isJdkDynamicProxy(cachedTable)) {
				String fullName = cachedTable.getClass().getGenericInterfaces()[0].getTypeName();
				String cacheKey = StringUtils.substringAfterLast(fullName, ".");
				ClassCachekeyMapping.put(className, cacheKey);
				return cacheKey;
			} else if (AopUtils.isCglibProxy(cachedTable)) {
				try {
					Field h = cachedTable.getClass().getDeclaredField("CGLIB$CALLBACK_0");
					h.setAccessible(true);
					Object dynamicAdvisedInterceptor = h.get(cachedTable);

					Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
					advised.setAccessible(true);

					Object target = ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();

					String cacheKey = target.getClass().getSimpleName();
					ClassCachekeyMapping.put(className, cacheKey);
					return cacheKey;
				} catch (Exception e) {
					throw new RuntimeException("unsupport CGLIB class: "+cachedTable,e);
				}
			}else{
				throw new RuntimeException("unknown proxy class: "+cachedTable);				
			}
		}
	}
}
