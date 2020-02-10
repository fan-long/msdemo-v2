package com.msdemo.v2.common.cache.sync.stream;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import com.msdemo.v2.common.cache.aspect.CacheQueryAspect;
import com.msdemo.v2.common.cache.config.CacheEnvHolder;
import com.msdemo.v2.common.cache.config.ParamCacheConstants;
import com.msdemo.v2.common.cache.core.CacheSyncDTO;
import com.msdemo.v2.common.cache.core.ICacheSyncSubscriber;

@ConditionalOnBean(CacheQueryAspect.class)
@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX, 
	name = ParamCacheConstants.PREFIX_SYNC_TYPE, havingValue="stream")
@EnableBinding(IParamSyncStreamInput.class)
public class Subscriber implements ICacheSyncSubscriber {
	
	private static final Logger logger =LoggerFactory.getLogger(Subscriber.class);
	@Autowired
	CacheEnvHolder holder;
	
	@StreamListener(IParamSyncStreamInput.PARAM_SYNC_INPUT)
	public void receive(List<CacheSyncDTO> message){
		this.subscribe(message);
	}
	
	@Override
	public void subscribe(List<CacheSyncDTO> modifiedCaches) {
		for (CacheSyncDTO dto: modifiedCaches){
			logger.info("received refresh notification of {}, value: {}",dto.getCacheKey(),dto.getValue() );
			if (holder.isCacheKeyEnabled(dto.getCacheKey())){
				holder.getStrategy(dto.getCacheKey()).refresh(dto.getCacheKey());
			}
		}
	}

}
