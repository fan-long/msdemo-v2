package com.msdemo.v2.resource.management.shutdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.utils.LogUtil;
import com.msdemo.v2.resource.management.zone.IManagedSwitchableService;
import com.msdemo.v2.resource.management.zone.ZoneStatusManager;

@Component
public class GracefulShutdownEventListener implements ApplicationListener<ContextClosedEvent> {

	private static Logger logger = LoggerFactory.getLogger(GracefulShutdownEventListener.class);

	private int shutdownDelay=5;  //seconds
	
	private static List<Object> customizeExecutors = new ArrayList<>();

	public static void register(Object executor){
		customizeExecutors.add(executor);
	}

	public void onApplicationEvent(ContextClosedEvent event) {
		ZoneStatusManager.setNodeActive(false);
		logger.warn("JVM is shutting down...");
		try {
			Map<String,IManagedSwitchableService> switchable = event.getApplicationContext()
					.getBeansOfType(IManagedSwitchableService.class);
			if (switchable!=null && switchable.size()>0){
				switchable.values().stream().forEach( s -> s.deactive());
			}
			TimeUnit.SECONDS.sleep(shutdownDelay);
			for (Object e:customizeExecutors){
				logger.info("shutdown {}",e);
				if (e instanceof ExecutorService)
					((ExecutorService) e).shutdown();
				else if (e instanceof ExecutorConfigurationSupport)
					((ExecutorConfigurationSupport) e).shutdown();
			}

		} catch (InterruptedException e) {
			LogUtil.exceptionLog(logger, e);
		}
	}
}
