package com.msdemo.v2.common.cache.core;

import java.util.ArrayList;
import java.util.List;

import com.msdemo.v2.common.ManagedThreadLocal;

public interface ICacheSyncPublisher {

	static final ManagedThreadLocal<List<CacheSyncDTO>> modifiedMapperList= 
			new ManagedThreadLocal<>(ICacheSyncPublisher.class.getSimpleName(),ArrayList.class);
	
	/**
	 * 发布参数变更
	 * 
	 */
	void publish(List<CacheSyncDTO> modifiedCaches);
	
	default void initModification(){
		if (modifiedMapperList.get()==null)
			modifiedMapperList.set(new ArrayList<>());
	}
	/**
	 * 记录当前交易执行过程中所有对缓存表的修改
	 * @param cacheKey
	 */
	default void recordModification(CacheSyncDTO modifiedCache){
		if (modifiedMapperList.get()!=null) modifiedMapperList.get().add(modifiedCache);
	}
	/**
	 * 发布缓存表并更通知，由缓存仓库触发已订阅的缓存节点刷新缓存
	 * @param currentCacheStore
	 */
	default void publishModification(List<CacheSyncDTO> modifiedCaches){
		if (modifiedCaches.size()>0)
			this.publish(modifiedCaches);
	} 
}
