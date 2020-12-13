package com.msdemo.v2.common.cache.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.core.IUniversalCache;

public class CacheEnvHolder implements InitializingBean {
	
	private static Logger logger = LoggerFactory.getLogger(CacheEnvHolder.class);
	
	private static Map<String, ICacheStoreStrategy> CachekeyStrategyMapping 
		= new HashMap<>();

	private static HashMap<String, String> ClassCachekeyMapping
		= new HashMap<>();
	
	private static HashMap<String,IUniversalCache<?>> CachekeyBeanMapping= new HashMap<>();
	
	private boolean initFlag;
	public CacheEnvHolder(boolean initFlag){
		this.initFlag=initFlag;
	}
	
	@Autowired
	CacheConfig paramConfig;
	
	@Autowired
	@Nullable
	IUniversalCache<?>[] cachedBeans;

	@Autowired
	ICacheStoreStrategy[] cacheStrategy;
	
	@Override
	public void afterPropertiesSet() {
		HashMap<String,String> cacheTypeMapping=paramConfig.getType();
		if (cachedBeans == null || cacheTypeMapping == null)
			return;
		for (IUniversalCache<?> cachedTable : cachedBeans) { 
			String cacheKey=getCacheKey(cachedTable);
			CachekeyBeanMapping.put(cacheKey, cachedTable);
			String cacheType = cacheTypeMapping.get(cacheKey);
			//TODO: 修改为并行加载
			for (ICacheStoreStrategy strategy : cacheStrategy) {
				if (StringUtils.equalsIgnoreCase(strategy.name(), cacheType)){
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
			if (strategy.name().equalsIgnoreCase(type)){
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
						CachekeyStrategyMapping.get(cacheKey).name(),
						strategy.name());				//enable new strategy
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
			String cacheType=CachekeyStrategyMapping.get(cacheKey).name();
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
	
	public static IUniversalCache<?> getCacheBean(String cacheKey){
		return CachekeyBeanMapping.get(cacheKey);
	}
	//use simple class name to identify cache key
	public static String getCacheKey(IUniversalCache<?> cachedTable) {
		String className=cachedTable.toString();
		if (ClassCachekeyMapping.containsKey(className)) {
			return ClassCachekeyMapping.get(className);
		} else {
			String cacheKey=cachedTable.name();
			ClassCachekeyMapping.put(className, cacheKey);
			return cacheKey;
		}
	}
}
