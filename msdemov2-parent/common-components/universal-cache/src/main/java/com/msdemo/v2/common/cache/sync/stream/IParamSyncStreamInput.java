package com.msdemo.v2.common.cache.sync.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;


public interface IParamSyncStreamInput {

	String PARAM_SYNC_INPUT = "param_sync_input";    
	
    @Input(PARAM_SYNC_INPUT)
    SubscribableChannel input();
}
