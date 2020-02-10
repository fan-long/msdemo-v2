package com.msdemo.v2.common.cache.core;

import java.util.List;

public interface ICacheSyncSubscriber {
	
	/**
	 * 订阅参数变更
	 * 
	 */
	void subscribe(List<CacheSyncDTO> modifiedCaches);

	
}
