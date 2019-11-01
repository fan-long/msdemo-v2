package com.msdemo.v2.common.verification;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class BaseHandlerBuilder<T extends IVerificationRule> {

	private IVerificationHandler<?> handlerInstance;	
	private List<T> rules;
	private LinkedHashMap<String,String[]> ruleCommands= new LinkedHashMap<>();

	public BaseHandlerBuilder(IVerificationHandler<?> handlerInstance, List<T> rules){
		this.handlerInstance=handlerInstance;
		this.rules=rules;
	}
	public BaseHandlerBuilder<T> rule(T rule){
		rules.add(rule);
		return this;
	}    	
	public BaseHandlerBuilder<T> rule(EnumRuleCommand<T> command){
		rules.add(command.getRule());
		this.ruleCommands.putIfAbsent(command.toString(), null);
		return this;
	}
	abstract public BaseHandlerBuilder<T> rule(String commandText);
	
	public BaseHandlerBuilder<T> rule(String commandText,String... params){
		this.ruleCommands.put(commandText, params);
		return this;
	}
	
	public IVerificationHandler<?> build(){
		return handlerInstance;
	}
	
	public HashMap<String,String[]> getRuleCommands(){
		return this.ruleCommands;
	}
	
	public interface EnumRuleCommand<T>{
		T getRule();
	}
	public interface IRuleFactory{}
}
