package com.msdemo.v2.common.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.msdemo.v2.common.cache.aspect.CacheQueryAspect;
import com.msdemo.v2.common.cache.aspect.CacheUpdateAspect;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.store.InMemoryCacheStore;
import com.msdemo.v2.common.cache.store.RedisCacheStore;
import com.msdemo.v2.common.cache.store.ThreadCacheStore;

@Configuration
//@EnableConfigurationProperties({ ParamConfig.class })
@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX, 
	name = ParamCacheConstants.PROPERTY_CACHE_ENABLED, matchIfMissing = true)
@EnableScheduling
public class ParamCacheAutoConfiguration {
		
	private static final Logger logger = LoggerFactory.getLogger(ParamCacheAutoConfiguration.class);

	@Bean
//	@RefreshScope
	public ParamConfig paramConfig(){
		return new ParamConfig();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX, name = ParamCacheConstants.PROPERTY_CACHE_QUERY, matchIfMissing = true)
	public CacheEnvHolder cacheEnvHolderInitLoad(){
		return new CacheEnvHolder(true);
	}
	
	@Bean
	@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX, name = ParamCacheConstants.PROPERTY_CACHE_QUERY, havingValue="false")
	public CacheEnvHolder cacheEnvHolder(){
		return new CacheEnvHolder(false);
	}
	
	@Bean
	@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX, name = ParamCacheConstants.PROPERTY_CACHE_QUERY, matchIfMissing = true)
	public CacheQueryAspect cacheQueryAspect() {
		logger.debug("create CacheQueryAspect bean");
		return new CacheQueryAspect();
	}

	@Bean
	@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX, name = ParamCacheConstants.PROPERTY_CACHE_UPDATE, matchIfMissing = true)
	public CacheUpdateAspect cacheUpdateAspect() {
		logger.debug("create CacheUpdateAspect bean");
		return new CacheUpdateAspect();
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
