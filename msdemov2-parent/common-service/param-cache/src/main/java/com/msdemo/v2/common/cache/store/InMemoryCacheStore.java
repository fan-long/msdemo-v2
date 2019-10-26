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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.msdemo.v2.common.cache.core.AbstractCachedObject;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.core.ICachedParamTable;
import com.msdemo.v2.common.util.ValueCopyUtils;

@SuppressWarnings({"unchecked"})
public class InMemoryCacheStore implements ICacheStoreStrategy {

	private static final Logger logger = LoggerFactory.getLogger(InMemoryCacheStore.class);

	private ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<>();

	// TODO create lock for each CacheItem
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	@Autowired
	ApplicationContext applicationContext;
	
	
	@Override
	public void putAll(String cacheKey, List<Object> records) {
			
		CacheItem cacheItem = Optional.ofNullable(cache.get(cacheKey)).orElseGet(() -> new CacheItem());
		cacheItem.allRecords = records;
		cacheItem.expired=false;
		for (String groupKey : cacheItem.index.keySet()) {
			cacheItem.index.put(groupKey, ValueCopyUtils.groupByKeys(cacheItem.allRecords, true, 
						ICacheStoreStrategy.KEY_DELIMITER,groupKey));
		}
		lock.writeLock().lock();
		cache.put(cacheKey, cacheItem);
		lock.writeLock().unlock();
	}

	@Override
	public <T> List<T> getAll(String cacheKey) {
		
		T returnType = null;
		if (!(returnType instanceof AbstractCachedObject)) 
			throw new RuntimeException("clone needed");
		
		lock.readLock().lock();
		List<T> records = (List<T>) cache.get(cacheKey).allRecords;
		lock.readLock().unlock();
		logger.debug("load {} records of {}", records.size(), cacheKey);

		return records;
	}
	
	@Override
	public boolean hasCache(String cacheKey){
		return cache.containsKey(cacheKey) && !cache.get(cacheKey).expired;
	}
	
	@Override
	public <T> T get(String cacheKey, String groupKey,boolean isList,boolean cloned, ProceedingJoinPoint pjd) {
		String params = StringUtils.join(pjd.getArgs(), KEY_DELIMITER);
		lock.readLock().lock();
		try {			
			
			if (!cache.get(cacheKey).index.containsKey(groupKey)){
				cache.get(cacheKey).index.put(groupKey, 
						ValueCopyUtils.groupByKeys(cache.get(cacheKey).allRecords, 
								true,
								ICacheStoreStrategy.KEY_DELIMITER,
								groupKey.split(ICacheStoreStrategy.KEY_DELIMITER)));
			}
			
			Object groupIndex= cache.get(cacheKey).index.get(groupKey)
					.get(params);
			
			if (groupIndex!=null) {
//				if (groupIndex instanceof List) {
					ArrayList<Object> result = new ArrayList<>(((List<Integer>)groupIndex).size());
					for (Integer i: (List<Integer>)groupIndex) 
						result.add(cloned  
								?((AbstractCachedObject<T>)cache.get(cacheKey).allRecords.get(i)).clone()
								: (T)cache.get(cacheKey).allRecords.get(i));
					if (logger.isDebugEnabled())
						logger.debug("load {} record(s) of {}-{}", 
								((List<?>)result).size(), cacheKey,groupKey);
					return isList ? (T) result : (T)result.get(0);
//				}else{
//					T result =(T) cache.get(cacheKey).allRecords.get((int)groupIndex);
//					if (logger.isDebugEnabled())
//						logger.debug("load {}-{}", cacheKey,groupKey);
//					return  cloned?((AbstractCachedObject<T>)result).clone(): (T)result ;
//				}
			}else{
				//TODO: result new List() if result type is List?
				return null;
			}
		} finally {
			lock.readLock().unlock();
		}

	}

	@Override
	public  void refresh(String cacheKey) {
		String[] paramTableMappers=applicationContext.getBeanNamesForType(ICachedParamTable.class);
		for (String mapperName: paramTableMappers){
			if (StringUtils.equalsIgnoreCase(mapperName,cacheKey)){
				ICachedParamTable<?> mapper = (ICachedParamTable<?>) applicationContext.getBean(mapperName);
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
				break;
			}
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

//	public  void publish(String[] cacheKey) {
//		syncAdapter.publish(cacheKey);
//	}
//	
//	public void subscribe() {
//		syncAdapter.subscribe();		
//	}

	@Override
	public String cacheType() {
		return "jvm";
	}

	
}
