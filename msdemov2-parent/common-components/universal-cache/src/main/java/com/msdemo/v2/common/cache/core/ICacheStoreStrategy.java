package com.msdemo.v2.common.cache.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;

import com.msdemo.v2.common.core.INameAwareBean;
import com.msdemo.v2.common.utils.ValueCopyUtil;

public interface ICacheStoreStrategy extends INameAwareBean{
	static String KEY_DELIMITER = ":";
		
	void putAll(String cacheKey, List<Object> records);

	<T> List<T> getAll(String cacheKey);

	boolean containsKey(String cacheKey);

	<T> T get(String cacheKey, CachedQuery annotation, ProceedingJoinPoint pjd) throws Throwable;

	void refresh(String cacheKey);

	Map<String,Map<String,Integer>> cacheInfo();
	
	void clear(String cacheKey);

	default boolean isPublishNeeded() { return true;};
	
	static String combineArgs(ProceedingJoinPoint pjd,CachedQuery annotation){
		if (annotation.dto()){
			if (pjd.getArgs().length!=1) throw new RuntimeException("only one DTO argument allowed!");{
				LinkedHashMap<String,Object> values=ValueCopyUtil.getValues(pjd.getArgs()[0], annotation.value());
				return StringUtils.join(values.values(), KEY_DELIMITER);
			}
		}else{
			return StringUtils.join(pjd.getArgs(), KEY_DELIMITER);
		}
	}
}
