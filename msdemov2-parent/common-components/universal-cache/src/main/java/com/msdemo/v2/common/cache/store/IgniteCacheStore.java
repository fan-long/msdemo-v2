package com.msdemo.v2.common.cache.store;

import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import com.msdemo.v2.common.cache.config.UniversalCacheConstants;
import com.msdemo.v2.common.cache.core.CachedQuery;
import com.msdemo.v2.common.cache.core.ICacheRedoObserver;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.resource.datasource.DynamicDataSource;

public class IgniteCacheStore implements ICacheStoreStrategy,ICacheRedoObserver{

	private static final Logger logger = LoggerFactory.getLogger(IgniteCacheStore.class);

	public static final String NAME=UniversalCacheConstants.IGNITE_DS_KEY;
	
	@Override
	public String name() {
		return NAME;
	}

	private void activate(boolean status){
		if (status)
			DynamicDataSource.setDataSource(UniversalCacheConstants.IGNITE_DS_KEY);
		else
			DynamicDataSource.setDataSource(null);
	}
	@Override
	public void putAll(String cacheKey, List<Object> records) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public <T> List<T> getAll(String cacheKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(String cacheKey) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(String cacheKey, CachedQuery annotation, ProceedingJoinPoint pjd) throws Throwable {
		this.activate(true);
		T result=(T) pjd.proceed();
		this.activate(false);
		return result;
	}

	@Override
	public void refresh(String cacheKey) {		
	}

	@Override
	public Map<String, Map<String, Integer>> cacheInfo() {
		return null;
	}

	@Override
	public void clear(String cacheKey) {
		
	}

	@Override
	public	boolean isPublishNeeded(){
		return false;
	}
	
	@Async
	@Override
	public void redo(ProceedingJoinPoint pjd) {
		try {
			this.activate(true);
			pjd.proceed();
			this.activate(false);
		} catch (Throwable e) {
			logger.error("ignite redo exception",e);
		}
	}

}
