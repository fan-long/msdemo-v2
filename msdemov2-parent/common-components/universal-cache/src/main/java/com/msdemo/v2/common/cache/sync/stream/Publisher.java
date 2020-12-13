package com.msdemo.v2.common.cache.sync.stream;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

import com.msdemo.v2.common.cache.aspect.CacheUpdateAspect;
import com.msdemo.v2.common.cache.config.UniversalCacheConstants;
import com.msdemo.v2.common.cache.core.CacheSyncDTO;
import com.msdemo.v2.common.cache.core.ICacheSyncPublisher;

@ConditionalOnBean(CacheUpdateAspect.class) 
@ConditionalOnProperty(prefix = UniversalCacheConstants.PREFIX, 
	name = UniversalCacheConstants.PREFIX_SYNC_TYPE, havingValue="stream")
@EnableBinding(IParamSyncStreamOutput.class)
public class Publisher implements ICacheSyncPublisher {

	@Autowired
	@Output(IParamSyncStreamOutput.PARAM_SYNC_OUTPUT)
	private MessageChannel publisherChannel;
	
	@Override
	public void publish(List<CacheSyncDTO> modifiedCaches) {
		publisherChannel.send(MessageBuilder.withPayload(modifiedCaches).build());
	}

}
