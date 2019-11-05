package com.msdemo.v2.common.cache.core;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;

import com.msdemo.v2.common.util.ValueCopyUtils;

public interface ICacheStoreStrategy {
	static String KEY_DELIMITER = ":";
	
	String cacheType();
	
	void putAll(String cacheKey, List<Object> records);

	<T> List<T> getAll(String cacheKey);

	boolean hasCache(String cacheKey);

	<T> T get(String cacheKey, CachedQuery annotation, ProceedingJoinPoint pjd) throws Throwable;

	void refresh(String cacheKey);

	Map<String,Map<String,Integer>> cacheInfo();
	
	void clear(String cacheKey);

	default boolean isPublishNeeded() { return true;};
	
	static String combineArgs(ProceedingJoinPoint pjd,CachedQuery annotation){
		if (annotation.dto()){
			if (pjd.getArgs().length!=1) throw new RuntimeException("only one DTO argument allowed!");
			return StringUtils.join(ValueCopyUtils.getValues(pjd.getArgs()[0], annotation.value()), KEY_DELIMITER);
		}else{
			return StringUtils.join(pjd.getArgs(), KEY_DELIMITER);
		}
	}
}
