package com.msdemo.v2.common.dtx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.msdemo.v2.common.dtx.compensation.CdtxStageCompensator;
import com.msdemo.v2.common.dtx.compensation.IStageCompensator;
import com.msdemo.v2.common.dtx.lock.ITxnLockAgent;
import com.msdemo.v2.common.dtx.lock.TxnLock;

public class ExpendableDtxConfigure {

	private static final Logger logger=LoggerFactory.getLogger(ExpendableDtxConfigure.class);

	@Bean
	@ConditionalOnMissingBean(ITxnLockAgent.class)
	public ITxnLockAgent txnLock(){
		logger.info("create TxnLock bean");
		return new TxnLock();
	}
	
	@Bean
	@ConditionalOnMissingBean(IStageCompensator.class)
	public IStageCompensator stageCompensator(){
		logger.info("create CdtxStageCompensator bean");
		return new CdtxStageCompensator();
	}
}
