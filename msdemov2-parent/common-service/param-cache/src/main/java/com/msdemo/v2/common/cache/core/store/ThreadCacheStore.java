package com.msdemo.v2.common.cache.core.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.ManagedThreadLocal;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;

public class ThreadCacheStore implements ICacheStoreStrategy {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private static ManagedThreadLocal<HashMap<String,Object>> threadCache = 
			new ManagedThreadLocal<>(ThreadCacheStore.class.getSimpleName(),HashMap.class);
	
	@Override
	public String cacheType() {
		return "thread";
	}

	@Override
	public void putAll(String cacheKey, List<Object> records) {
		
	}

	@Override
	public <T> List<T> getAll(String cacheKey) {
		return null;
	}

	@Override
	public boolean hasCache(String cacheKey) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(String cacheKey, String groupFields,boolean isList, boolean prototype, ProceedingJoinPoint pjd) {
		HashMap<String,Object> cache= threadCache.get();
		String params = cacheKey+KEY_DELIMITER+StringUtils.join(pjd.getArgs(), KEY_DELIMITER);
		if (cache.containsKey(params)){
			logger.trace("load  {}", params);
			return (T) cache.get(params);
		}
		else{
			try {
				cache.put(params, (T) pjd.proceed());
				threadCache.set(cache);
				return (T) cache.get(params);
			} catch (Throwable e) {
				logger.error(e.getMessage());
				throw new RuntimeException(e.getMessage());
			}			
		}
	}

	@Override
	public void refresh(String cacheKey) {
		clear(cacheKey);
	}

	@Override
	public void clear(String cacheKey) {
		threadCache.remove();
	}
	
	@Override
	public	boolean isPublishNeeded(){
		return false;
	}
	
	@Override
	public Map<String, Map<String, Integer>> cacheInfo() {
		return null;
	}

}
