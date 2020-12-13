package com.msdemo.v2.common.dtx;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.dtx.compensation.CdtxCoordinator;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.timing.domain.TimingEventListener;
import com.msdemo.v2.common.timing.domain.ZoneAwareTimingEventListener;

@Configuration
public class DtxConfiguration {

	public static final String CONFIG_PREFIX=CommonConstants.CONFIG_ROOT_PREFIX+".dtx";
	
	public static final String DTX_TIMEOUT_KEY="DTX-TIMEOUT";
	
	@Bean(DTX_TIMEOUT_KEY)
	public ZoneAwareTimingEventListener listener() {
		return new ZoneAwareTimingEventListener();
	}
	
	@Bean
	public TimingEventListener dtxExpirationListener() {
		return new TimingEventListener(DTX_TIMEOUT_KEY,DTX_TIMEOUT_KEY);
	}
	
	@Bean
	public IDtxCoordinator cdtxCoordinator(){
		CdtxCoordinator cdtx= new CdtxCoordinator(dtxExpirationListener());
		cdtx.getExpirationListener().setCallback(cdtx);
		cdtx.getExpirationListener().setGroupTimerKey(DTX_TIMEOUT_KEY);
		return cdtx;
	}
	
}
