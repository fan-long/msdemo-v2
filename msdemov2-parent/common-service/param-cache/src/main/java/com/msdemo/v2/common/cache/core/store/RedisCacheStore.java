package com.msdemo.v2.common.cache.core.store;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
@SuppressWarnings("unchecked")
public class RedisCacheStore implements ICacheStoreStrategy {
	private static final Logger logger = LoggerFactory.getLogger(RedisCacheStore.class);

	@Autowired
	RedisTemplate<String,Object> redis;

	@Override
	public void putAll(String cacheKey, List<Object> records) {
		redis.boundHashOps(cacheKey).put(null, null);
	}

	@Override
	public <T> List<T> getAll(String cacheKey) {
		return null;
	}

	@Override
	public boolean hasCache(String cacheKey) {
		//always return true 
		return true;
	}

	@Override
	public <T> T get(String cacheKey, String groupFields, boolean isList, boolean prototype, ProceedingJoinPoint pjd) {
		//FIXME: same key issue if different fields combination
		String params = StringUtils.join(pjd.getArgs(), KEY_DELIMITER);
		T result = (T) redis.boundHashOps(cacheKey).get(params);
		if (result ==null)		{	
			try {
				result = (T) pjd.proceed();
			} catch (Throwable e) {
				logger.error(e.getMessage());
				result =null;
			}
			redis.boundHashOps(cacheKey).put(params, result);
		}else{
			logger.trace("load  {}", params);
		}
		return result;
	}

	@Override
	public void refresh(String cacheKey) {
		//do nothing since redis cache should be refresh by Sync Adapter
//		clear(cacheKey);
	}

	@Override
	public void clear(String cacheKey) {
		redis.delete(cacheKey);
	}
	
	@Override
	public Map<String, Map<String, Integer>> cacheInfo() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public	boolean isPublishNeeded(){
		return false;
	}
	@Override
	public String cacheType() {
		return "redis";
	}
}
