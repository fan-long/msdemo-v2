package com.msdemo.v2.resource.management;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.util.LogUtils;

@Component
public class GracefulShutdownService implements ApplicationListener<ContextClosedEvent> {
	private Logger logger = LoggerFactory.getLogger(GracefulShutdownService.class);

	@Override
	public void onApplicationEvent(ContextClosedEvent arg0) {
		logger.warn("JVM is shutting down...");
		CommonConstants.JVM_RUNNING_FLAG.set(false);
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			LogUtils.exceptionLog(logger, e);;
		}
	}

}
