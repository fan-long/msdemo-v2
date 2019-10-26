package com.msdemo.v2.common.cache.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.msdemo.v2.common.cache.core.store.InMemoryCacheStore;
import com.msdemo.v2.common.cache.core.store.RedisCacheStore;
import com.msdemo.v2.common.cache.core.store.ThreadCacheStore;
import com.msdemo.v2.common.cache.core.sync.ConsulAPISyncAdapter;

@Configuration
//@EnableConfigurationProperties({ ParamConfig.class })
@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX_STORE, name = ParamCacheConstants.PROPERTY_CACHE_ENABLED, havingValue="true", matchIfMissing = true)
@EnableScheduling
public class ParamCacheAutoConfiguration {
		
	private static final Logger logger = LoggerFactory.getLogger(ParamCacheAutoConfiguration.class);

	//TODO: test cache enabled but both query and update disabled
	@Bean
	public ParamConfig paramConfig(){
		return new ParamConfig();
	}
	
	@Bean
	public CacheEnvHolder cacheEnvHolder(){
		return new CacheEnvHolder();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX_STORE, name = ParamCacheConstants.PROPERTY_CACHE_QUERY, matchIfMissing = true)
	public CacheQueryAspect cacheQueryAspect() {
		logger.debug("create CacheQueryAspect bean");
		return new CacheQueryAspect();
	}

	@Bean
	@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX_STORE, name = ParamCacheConstants.PROPERTY_CACHE_UPDATE, matchIfMissing = true)
	public CacheUpdateAspect cacheUpdateAspect() {
		logger.debug("create CacheUpdateAspect bean");
		return new CacheUpdateAspect();
	}
	
	@Bean
	public ICacheSyncStrategy consulSyncAdapter() {
		logger.debug("create Param-Cache sync bean via Consul API");
		return new ConsulAPISyncAdapter();
//		logger.debug("create Param-Cache sync bean via Consul Client");
//		return new ConsulClientSyncAdapter();
	}
	
	@Bean
	public ICacheStoreStrategy inMemoryCacheStore() {
		logger.debug("create inMemoryCacheStore bean");
		return new InMemoryCacheStore();
	}
	
	@Bean
	public ICacheStoreStrategy redisCacheStore() {
		logger.debug("create redisCacheStore bean");
		return new RedisCacheStore();
	}
	
	@Bean
	public ICacheStoreStrategy threadCacheStore() {
		logger.debug("create threadCacheStore bean");
		return new ThreadCacheStore();
	}
}
