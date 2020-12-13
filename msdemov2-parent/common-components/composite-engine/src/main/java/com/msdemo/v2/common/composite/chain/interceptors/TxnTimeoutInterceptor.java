package com.msdemo.v2.common.composite.chain.interceptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.chain.AbsTxnInterceptor;
import com.msdemo.v2.common.context.TransContext;

public class TxnTimeoutInterceptor extends AbsTxnInterceptor {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	protected CompositionContext intercept(InterceptorModel model) {
		CompositionContext result = super.intercept(model);
		if (!TransContext.isDistributedTransaction()){ // only for local transaction, dtx expiration check on process interceptor
			TransContext.verifyDeadline();
			logger.info("expiration check passed, transaction is being committed...");
		}
		return result;
	}
}
