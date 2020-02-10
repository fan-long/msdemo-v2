package com.msdemo.v2.common.cache.store;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import com.msdemo.v2.common.cache.config.CacheEnvHolder;
import com.msdemo.v2.common.cache.core.CacheSyncDTO;
import com.msdemo.v2.common.cache.core.CachedQuery;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.core.ICacheSyncPublisher;
import com.msdemo.v2.common.cache.core.ICachedParamTable;
import com.msdemo.v2.common.util.ValueCopyUtils;
@SuppressWarnings("unchecked")
public class RedisCacheStore implements ICacheStoreStrategy {
	private static final Logger logger = LoggerFactory.getLogger(RedisCacheStore.class);

	@Autowired
	@Nullable
	RedisTemplate<String,Object> redis;
	
	@Override
	public void putAll(String cacheKey, List<Object> records) {
//		redis.boundHashOps(cacheKey).put(null, null);
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
	public <T> T get(String cacheKey, CachedQuery annotation, ProceedingJoinPoint pjd) throws Throwable {
		if (redis==null) return (T) pjd.proceed();
		String params = ICacheStoreStrategy.combineArgs(pjd,annotation);
		T result = (T) redis.boundHashOps(cacheKey).get(params);
		if (result ==null){	
			result = (T) pjd.proceed();
			redis.boundHashOps(cacheKey).put(params, result);
			logger.trace("load {}, result {}", params,result);
		}
		return result;
	}

	@Override
	public void refresh(String cacheKey) {
		if (redis==null) return;
		if (ICacheSyncPublisher.modifiedMapperList!=null){
			for (CacheSyncDTO dto:ICacheSyncPublisher.modifiedMapperList.get()){
				if (StringUtils.equals(dto.getCacheKey(),cacheKey) &&
						dto.getAction()!=null){
					ICachedParamTable<?> mapper= CacheEnvHolder.getCacheMapper(cacheKey);
					//delete all related keys. Delete is an safe action since it's difficult to check nested cache item
					//only support MyBatis dynamic-proxy mapper
					for (Method method:mapper.getClass().getInterfaces()[0].getDeclaredMethods()){
						if (method.isAnnotationPresent(CachedQuery.class)){
							for (Object modifiedValue:dto.getValue())
							{
								redis.boundHashOps(cacheKey).delete(
									StringUtils.join(ValueCopyUtils.getValues(modifiedValue, 
											method.getAnnotation(CachedQuery.class).value()).values(), KEY_DELIMITER));
							}
						}
					}
					logger.info("refresh cachekey: {}, action: {} value: {}",cacheKey,dto.getAction(), dto.getValue());
//					switch (dto.getAction()){
//						case INSERT:
//							break;
//						case UPDATE:
//							break;
//						case DELETE:
//							break;
//					}
				}
			}
		}
	}

	@Override
	public void clear(String cacheKey) {
		if (redis!=null) redis.delete(cacheKey);
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
