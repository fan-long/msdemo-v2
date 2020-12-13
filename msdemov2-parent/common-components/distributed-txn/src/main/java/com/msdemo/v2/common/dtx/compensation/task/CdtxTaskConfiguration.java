package com.msdemo.v2.common.dtx.compensation.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.msdemo.v2.async.AsyncEngineService;
import com.msdemo.v2.common.dtx.DtxConfiguration;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.resource.management.zone.AbsSwitchableService;
import com.msdemo.v2.resource.management.zone.ZoneAwareResourceHolder;

@Configuration
public class CdtxTaskConfiguration {

	@Bean
	CdtxExpirationMonitorTask cdtxExpirationMonitorTask(){
		return new CdtxExpirationMonitorTask();
	}
	@Bean
	CdtxRecompenseTask cdtxRecompenseTask(){
		return new CdtxRecompenseTask();
	}
	
	@Component
	@ConditionalOnProperty(prefix = DtxConfiguration.CONFIG_PREFIX+".compensator", name = "scheduled", havingValue = "true")
	public static class CdtxTaskScheduler extends AbsSwitchableService{
		private static final Logger logger=LoggerFactory.getLogger(CdtxTaskScheduler.class);
		
		@Autowired
		@Lazy //important for SwitchableBeanPostBuilder to avoid TimingEventListener early initialization
		AsyncEngineService taskService;
		
		@Scheduled(fixedRate=IDtxCoordinator.DEFAULT_TIME_OUT_MILLIS*3)
		public void expirationMonitorTask(){
			if (isActive()){
				ZoneAwareResourceHolder.bindZoneId(getZoneId());
				IDtxCoordinator.applyTrace(IDtxCoordinator.DTX_ID_PREFIX);
				logger.trace("run task of CdtxExpirationMonitorTask on {}",getZoneId());
				taskService.startTask(CdtxExpirationMonitorTask.class.getSimpleName(),
					false);
			}
		}
		
		@Scheduled(fixedRate=IDtxCoordinator.DEFAULT_TIME_OUT_MILLIS)
		public void recompenseTask(){
			if (isActive()){
				ZoneAwareResourceHolder.bindZoneId(getZoneId());
				IDtxCoordinator.applyTrace(IDtxCoordinator.DTX_ID_PREFIX);
				logger.trace("run task of CdtxRecompenseTask on {}",getZoneId());
				taskService.startTask(CdtxRecompenseTask.class.getSimpleName(),
					false);
			}
		}

		@Override
		protected void doActivate() {
		}
	}
}
