package com.msdemo.v2.common.cache.config;

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

import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.core.ICachedParamTable;

public class CacheEnvHolder implements InitializingBean {
	
	private static Logger logger = LoggerFactory.getLogger(CacheEnvHolder.class);
	
	private static Map<String, ICacheStoreStrategy> CachekeyStrategyMapping 
		= new HashMap<>();

	private static HashMap<String, String> ClassCachekeyMapping
		= new HashMap<>();
	
	private static HashMap<String,ICachedParamTable<?>> CachekeyMapperMapping= new HashMap<>();
	
	private boolean initFlag;
	public CacheEnvHolder(boolean initFlag){
		this.initFlag=initFlag;
	}
	
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
			CachekeyMapperMapping.put(cacheKey, cachedTable);
			String cacheType = cacheTypeMapping.get(cacheKey);
			//TODO: 修改为并行加载
			for (ICacheStoreStrategy strategy : cacheStrategy) {
				if (StringUtils.equalsIgnoreCase(strategy.cacheType(), cacheType)){
					CachekeyStrategyMapping.put(cacheKey, strategy);
					//启动时初始化加载缓存
					if (initFlag) strategy.refresh(cacheKey);
				}
			}
		}
		paramConfig.linkToHolder(this);
	}

	ICacheStoreStrategy getStrategyByType(String type){
		for (ICacheStoreStrategy strategy : cacheStrategy) {
			if (strategy.cacheType().equalsIgnoreCase(type)){
				return strategy;
			}
		}
		return null;
	}
//	public boolean isConfigEnabled(String cacheKey){
//		return paramConfig.getType().containsKey(cacheKey) 
//				&& !StringUtils.equalsIgnoreCase(paramConfig.getType().get(cacheKey),ParamConfig.CACHEKEY_DISABLED);
//	}
	
	public boolean isCacheKeyEnabled(String cacheKey){
		return CachekeyStrategyMapping.containsKey(cacheKey);		
	}
	
	public ICacheStoreStrategy getStrategy(String cacheKey){
		return CachekeyStrategyMapping.get(cacheKey);
	} 
	
	void changeCacheType(String cacheKey,String cacheType){
		ICacheStoreStrategy strategy = getStrategyByType(cacheType);
		if (strategy==null){ //invalid strategy type
			if (isCacheKeyEnabled(cacheKey)){
				disableCache(cacheKey);
			}else{
				logger.info("ingored type: {} for cache: {}",cacheKey,cacheType);
			}
		}else{ //valid strategy type
			if (isCacheKeyEnabled(cacheKey)){
				//clear cache of old strategy. 
				//NOTES: Redis cache would be cleared more than 1 time by all nodes 
				CachekeyStrategyMapping.get(cacheKey).clear(cacheKey);
				logger.info("replace cache type of {} from {} to {}",cacheKey,
						CachekeyStrategyMapping.get(cacheKey).cacheType(),
						strategy.cacheType());				//enable new strategy
				//TODO: empty cached data during refresh
				// consider to allow native query in CacheQueryAspect.cachedQuery during refresh period?
				CachekeyStrategyMapping.put(cacheKey, strategy);
				//load cache with new strategy
				strategy.refresh(cacheKey);

			}else{
				enableCache(cacheKey,cacheType);
			}	
		}
	}
	void disableCache(String cacheKey){
		if (isCacheKeyEnabled(cacheKey)){
			String cacheType=CachekeyStrategyMapping.get(cacheKey).cacheType();
			CachekeyStrategyMapping.get(cacheKey).clear(cacheKey);
			CachekeyStrategyMapping.remove(cacheKey);
			logger.info("disabled cache of {} with type: {}", cacheKey,cacheType);
		}
	}
	void enableCache(String cacheKey,String cacheType){
		ICacheStoreStrategy strategy = getStrategyByType(cacheType);
		if (strategy!=null){
			CachekeyStrategyMapping.put(cacheKey, strategy);
			strategy.refresh(cacheKey);
			logger.info("enabled cache of {} with type: {}", cacheKey,cacheType);
		}else
			logger.info("ingored type: {} for cache: {}",cacheKey,cacheType);
	}
	
	public static ICachedParamTable<?> getCacheMapper(String cacheKey){
		return CachekeyMapperMapping.get(cacheKey);
	}
	//use simple class name to identify cache key
	public static String getCacheKey(ICachedParamTable<?> cachedTable) {
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
