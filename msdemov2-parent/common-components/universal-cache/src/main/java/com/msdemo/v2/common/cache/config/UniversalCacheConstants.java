package com.msdemo.v2.common.cache.config;

import com.msdemo.v2.common.CommonConstants;

public interface UniversalCacheConstants {
	

	static final String PREFIX = CommonConstants.CONFIG_ROOT_PREFIX+
			".cache";
	static final String PROPERTY_CACHE_ENABLED = "enabled";

	static final String PREFIX_SYNC_TYPE = "sync.type";
	static final String PREFIX_SYNC_CONSUL=PREFIX+".sync.consul.path";
	static final String PREFIX_SYNC_CONSUL_WATCH=PREFIX+".sync.consul.watch-delay";

	static final String PROPERTY_CACHE_QUERY = "mode.query";
	static final String PROPERTY_CACHE_UPDATE = "mode.update";

	static final String PRIMAY_DS_KEY="primary";
	static final String READONLY_DS_KEY="read";
	static final String IGNITE_DS_KEY="ignite";
	
}
