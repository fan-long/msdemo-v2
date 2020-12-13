package com.msdemo.v2.common.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.cache.aspect.CacheQueryAspect;
import com.msdemo.v2.common.cache.aspect.CacheUpdateAspect;
import com.msdemo.v2.common.cache.core.ICacheStoreStrategy;
import com.msdemo.v2.common.cache.core.ICacheSyncPublisher;
import com.msdemo.v2.common.cache.core.ICacheSyncSubscriber;
import com.msdemo.v2.common.cache.store.IgniteCacheStore;
import com.msdemo.v2.common.cache.store.InMemoryCacheStore;
import com.msdemo.v2.common.cache.store.RedisCacheStore;
import com.msdemo.v2.common.cache.store.ThreadCacheStore;
import com.msdemo.v2.common.cache.sync.consul.Publisher;
import com.msdemo.v2.common.cache.sync.consul.Subscriber;

@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, 
	name = UniversalCacheConstants.PROPERTY_CACHE_ENABLED, matchIfMissing = true)
@EnableScheduling
public class UniversalCacheAutoConfiguration {
		
	private static final Logger logger = LoggerFactory.getLogger(UniversalCacheAutoConfiguration.class);

	@Bean
//	@RefreshScope
	public CacheConfig cacheConfig(){
		return new CacheConfig();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, name = UniversalCacheConstants.PROPERTY_CACHE_QUERY, matchIfMissing = true)
	public CacheEnvHolder cacheEnvHolderInitLoad(){
		return new CacheEnvHolder(true);
	}
	
	@Bean
	@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, name = UniversalCacheConstants.PROPERTY_CACHE_QUERY, havingValue="false")
	public CacheEnvHolder cacheEnvHolder(){
		return new CacheEnvHolder(false);
	}
	
	@Bean
	@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, name = UniversalCacheConstants.PROPERTY_CACHE_QUERY, matchIfMissing = true)
	public CacheQueryAspect cacheQueryAspect() {
		logger.debug("create CacheQueryAspect bean");
		return new CacheQueryAspect();
	}

	@Bean
	@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, name = UniversalCacheConstants.PROPERTY_CACHE_UPDATE, matchIfMissing = true)
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
	
	@Bean
	@ConditionalOnProperty(prefix = CommonConstants.CONFIG_ROOT_PREFIX, name = UniversalCacheConstants.IGNITE_DS_KEY)	
	public ICacheStoreStrategy igniteCacheStore() {
		logger.debug("Ignite is enabled, create igniteCacheStore bean");
		return new IgniteCacheStore();
	}
	
	public static class ConsulSyncConfigure{
		
		@ConditionalOnBean(CacheUpdateAspect.class)
		@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, 
			name = UniversalCacheConstants.PREFIX_SYNC_TYPE, havingValue="consul")
		@Bean
		ICacheSyncPublisher consulSyncPublish(){
			return new Publisher();
		}
		
		@ConditionalOnBean(CacheQueryAspect.class)
		@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, 
			name = UniversalCacheConstants.PREFIX_SYNC_TYPE, havingValue="consul")
		@Bean 
		ICacheSyncSubscriber consulSyncSubscriber(){
			return new Subscriber();
		}

	}
}
