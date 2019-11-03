package com.msdemo.v2.common.verification;

import java.util.List;

import com.msdemo.v2.common.core.IVerificationAware;

public interface IVerificationHandler<T extends IVerificationRule> {
	
	 default void handle(List<T> rules, IVerificationAware reqDto){
		 for (T rule:rules){
			 rule.check(reqDto);
		 }
	 };
	 default void handle(IVerificationAware reqDto,Object context){
		 handle(getBuilder().getRunnableRules(context),reqDto);		 
	 }
	 
	 /**
	  * Factory of Rules
	  * @return
	  */
	 BaseHandlerBuilder.IRuleFactory ruleFactory();
	 
	 BaseHandlerBuilder<T> getBuilder();

}
