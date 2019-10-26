package com.msdemo.v2.common.cache.core;

import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;

public interface ICacheStoreStrategy {
	static String KEY_DELIMITER = "-";
	
	String cacheType();
	
	void putAll(String cacheKey, List<Object> records);

	<T> List<T> getAll(String cacheKey);

	boolean hasCache(String cacheKey);

	<T> T get(String cacheKey, String groupFields,boolean isList,boolean prototype, ProceedingJoinPoint pjd);

	void refresh(String cacheKey);

	Map<String,Map<String,Integer>> cacheInfo();
	
	void clear(String cacheKey);

	default boolean isPublishNeeded() { return true;};
}
