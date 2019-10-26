package com.msdemo.v2.common.cache.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.msdemo.v2.common.ManagedThreadLocal;

public interface ICacheSyncStrategy {
	
	static final ManagedThreadLocal<Set<String>> modifiedMapperList= 
			new ManagedThreadLocal<>(ICacheSyncStrategy.class.getSimpleName(),HashSet.class);


	/**
	 * 接收配置中心的参数变更
	 * 
	 */
	void subscribe();

	/**
	 * 向配置中心发送参数变更通知
	 * @param cacheKey
	 */
	void publish(ArrayList<String> cacheKey);
	
	default void initModification(){
		if (modifiedMapperList.get()==null)
			modifiedMapperList.set(new HashSet<>());
	}
	/**
	 * 记录当前交易执行过程中所有对缓存表的修改
	 * @param cacheKey
	 */
	default void recordModification(String cacheKey){
		if (modifiedMapperList.get()!=null) modifiedMapperList.get().add(cacheKey);
	}
	/**
	 * 发布缓存表并更通知，由缓存仓库触发已订阅的缓存节点刷新缓存
	 * @param currentCacheStore
	 */
	default void publishModification(ArrayList<String> cacheKey){
		if (cacheKey!=null && cacheKey.size()>0)
			this.publish(cacheKey);
	} 
}
