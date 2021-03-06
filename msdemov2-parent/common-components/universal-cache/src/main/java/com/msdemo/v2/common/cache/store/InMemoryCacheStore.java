package com.msdemo.v2.common.cache.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.cache.config.CacheEnvHolder;
import com.msdemo.v2.common.cache.core.AbstractCachedObject;
import com.msdemo.v2.common.cache.core.CachedQuery;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.core.IUniversalCache;
import com.msdemo.v2.common.utils.ValueCopyUtil;

@SuppressWarnings({"unchecked"})
public class InMemoryCacheStore implements ICacheStoreStrategy {

	private static final Logger logger = LoggerFactory.getLogger(InMemoryCacheStore.class);

	public static final String NAME="jvm";
	
	private ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<>();

	//create lock for each CacheItem
	private Map<String,ReentrantReadWriteLock> lock = new HashMap<>();
	
	@Override
	public void putAll(String cacheKey, List<Object> records) {
		
		ReentrantReadWriteLock locker= Optional.ofNullable(lock.get(cacheKey))
				.orElseGet(() ->{ 
					lock.put(cacheKey, new ReentrantReadWriteLock()); 
					logger.debug("create ReentrantReadWriteLock for cache: {}", cacheKey);
					return lock.get(cacheKey);
					});
			
		CacheItem cacheItem = Optional.ofNullable(cache.get(cacheKey)).orElseGet(() -> new CacheItem());
		cacheItem.allRecords = records;
		cacheItem.expired=false;
		for (String groupKey : cacheItem.index.keySet()) {
			cacheItem.index.put(groupKey, ValueCopyUtil.groupByKeys(cacheItem.allRecords, true, 
						ICacheStoreStrategy.KEY_DELIMITER,groupKey));
		}
		locker.writeLock().lock();
		cache.put(cacheKey, cacheItem);
		locker.writeLock().unlock();
	}

	@Override
	public <T> List<T> getAll(String cacheKey) {
			
		lock.get(cacheKey).readLock().lock();
		List<T> records = (List<T>) cache.get(cacheKey).allRecords;
		lock.get(cacheKey).readLock().unlock();
		logger.debug("load {} records of {}", records.size(), cacheKey);

		return records;
	}
	
	@Override
	public boolean containsKey(String cacheKey){
		return cache.containsKey(cacheKey) && !cache.get(cacheKey).expired;
	}
	
	@Override
	public <T> T get(String cacheKey, CachedQuery annotation, ProceedingJoinPoint pjd) throws Throwable{
		// TODO: CacheEnvHolder中获取所有@CachedQuery类方法，并缓存方法参数名，参考CacheTest			
		if (annotation.value()==null) throw new RuntimeException("fields is required on CachedQuery");
		String params = ICacheStoreStrategy.combineArgs(pjd,annotation);
		MethodSignature ms = (MethodSignature) pjd.getSignature();
		//如果getReturnType instanceof ParameterizedType，通过getActualTypeArguments可获取实际类型
		boolean isList = ms.getMethod().getReturnType().isAssignableFrom(List.class);
		// TODO: group key would be DTO or expected fields
		// do NOT support different query method with same parameters
		String groupKey= StringUtils.join(annotation.value(),ICacheStoreStrategy.KEY_DELIMITER);
		lock.get(cacheKey).readLock().lock();
		try {			
			if (!cache.get(cacheKey).index.containsKey(groupKey)){
				cache.get(cacheKey).index.put(groupKey, 
						ValueCopyUtil.groupByKeys(cache.get(cacheKey).allRecords, 
								true,
								ICacheStoreStrategy.KEY_DELIMITER,
								groupKey.split(ICacheStoreStrategy.KEY_DELIMITER)));
			}
			
			Object groupIndex= cache.get(cacheKey).index.get(groupKey)
					.get(params);
			
			if (groupIndex!=null) {
					ArrayList<T> result = new ArrayList<>(((List<Integer>)groupIndex).size());
					for (Integer i: (List<Integer>)groupIndex) 
						result.add(annotation.cloned()
								?((AbstractCachedObject<T>)cache.get(cacheKey).allRecords.get(i)).clone()
								: (T)cache.get(cacheKey).allRecords.get(i));
					if (logger.isDebugEnabled())
						logger.debug("load {} record(s) of {}-{}", 
								((List<?>)result).size(), cacheKey,groupKey);
					return isList ? (T) result : (T)result.get(0);
			}else{
				if (annotation.pernetrate())
					return (T) pjd.proceed();					
				//TODO: result new List() if result type is List?
				return null;
			}
		} finally {
			lock.get(cacheKey).readLock().unlock();
		}

	}

	@Override
	public  void refresh(String cacheKey) {
		IUniversalCache<?> mapper = CacheEnvHolder.getCacheBean(cacheKey);
		if (mapper!=null){
			CacheItem cache= Optional.ofNullable(this.cache.get(cacheKey))
						.orElseGet(()->{
							CacheItem item=new CacheItem();
							this.cache.put(cacheKey, item);
							return item;
						});
			cache.expired=true;
			List<Object> records=(List<Object>) mapper.selectAll();
			this.putAll(cacheKey, records);
			logger.info("refreshed {} record(s) by {}", records!=null?records.size():0, cacheKey);
		}else{
			logger.warn("ignored cache refresh event of {}, no mapper found.", cacheKey);			
		}
	}
	@Override
	public  void clear(String cacheKey) {
		this.cache.remove(cacheKey);
		logger.info("removed {} cache in JVM",cacheKey);
	}
	
	@Override
	public Map<String, Map<String, Integer>> cacheInfo() {
		Map<String, Map<String, Integer>> result  = new HashMap<>();
		for (String cacheKey:this.cache.keySet()){
			Map<String, Integer> items= new HashMap<>();
			for (String itemName: this.cache.get(cacheKey).index.keySet()){
				items.put("["+itemName+"]", this.cache.get(cacheKey).index.get(itemName).size());
			}
			items.put("RECORDS", this.cache.get(cacheKey).allRecords.size());
			result.put(cacheKey, items);
		}
		return result;
	}	
	
	private static class CacheItem {
		private boolean expired;
		private List<?> allRecords;
		// 按照查询条件的参数名称分类，每类按组合后的键值索引指向allRecords
		private HashMap<String, HashMap<String, Object>> index = new HashMap<>();
	}

	@Override
	public String name() {
		return NAME;
	}

}
