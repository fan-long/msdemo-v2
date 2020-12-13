package com.msdemo.v2.common.cache.sync.stream;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface IParamSyncStreamOutput {
	String PARAM_SYNC_OUTPUT = "param_sync_output";

	@Output(PARAM_SYNC_OUTPUT)
    MessageChannel output();
}
