package com.msdemo.v2.common.cache.core;

import com.msdemo.v2.common.CommonConstants;

public class ParamCacheConstants {
	

	public static final String PREFIX = CommonConstants.CONFIG_ROOT_PREFIX+
			".param-cache";
	public static final String PREFIX_STORE = PREFIX+".store";

	public static final String PREFIX_SYNC_CONSUL=PREFIX+".sync.consul-path";
	public static final String PREFIX_SYNC_CONSUL_WATCH=PREFIX+".sync.consul-watch-delay";

	public static final String PROPERTY_CACHE_ENABLED = "enabled";
	
	public static final String PROPERTY_CACHE_QUERY = "mode.query";
	public static final String PROPERTY_CACHE_UPDATE = "mode.update";

//	public enum CacheType{
//		JVM("jvm"),REDIS("redis"),THREAD("thread");
//		private String type;
//		CacheType(String type){
//			this.setType(type);
//		}
//		public String getType() {
//			return type;
//		}
//		public void setType(String type) {
//			this.type = type;
//		}
//	}
	
}
