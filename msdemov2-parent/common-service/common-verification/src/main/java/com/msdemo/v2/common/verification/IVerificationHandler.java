package com.msdemo.v2.common.verification;

import com.msdemo.v2.common.core.IVerificationAware;

public interface IVerificationHandler<T extends IVerificationRule> {
	 default void handle(T[] rules, IVerificationAware context){};
	 void handle(IVerificationAware context);
	 BaseHandlerBuilder.IRuleFactory registerRuleFactory();
	 
	 BaseHandlerBuilder<?> getBuilder();

}
