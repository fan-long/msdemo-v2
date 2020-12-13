package com.msdemo.v2.common.verification.chain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;

import com.msdemo.v2.common.verification.IVerificationRule;

public abstract class BaseHandlerBuilder<T extends IVerificationRule> {

	private IVerificationHandler<?> handlerInstance;	
	/**
	 * all defined rules
	 */
	private List<T> definedRules = new ArrayList<>();
	/**
	 * rule command with runtime parameters
	 */
	private List<MutablePair<String,Object[]>> ruleCommands= new ArrayList<>();

	private boolean dynamicFlag;
	
	public BaseHandlerBuilder(IVerificationHandler<?> handlerInstance){
		this.handlerInstance=handlerInstance;
	}
	protected BaseHandlerBuilder<T> rule(T rule){
		definedRules.add(rule);
		return this;
	}    	

	public BaseHandlerBuilder<T> rule(EnumRuleCommand<T> command){
		rule(command.getRule());
		this.ruleCommands.add(new MutablePair<>(command.toString(), null));
		return this;
	}
	
	public BaseHandlerBuilder<T> rule(String commandText){
		return rule(commandText.toUpperCase(),(Object[])null);
	}
		
	public BaseHandlerBuilder<T> rule(EnumRuleCommand<T> command,Object... params){
		return rule(command.toString(),params);
	}
	
	abstract protected T parameterizedRule(String commandText,Object... params);

	public BaseHandlerBuilder<T> rule(String commandText,Object... params){
		if (params!=null){
			for (Object param:params){
				if (param instanceof IVerificationParam){
					dynamicFlag=true;
					break;
				}				
			}
		}
		parameterizedRule(commandText,params);
		this.ruleCommands.add(new MutablePair<>(commandText, params));
		return this;
	}
	
	public List<T> getRunnableRules(Object paramContext){
		if (!dynamicFlag) return definedRules;
		//rebuild rules with runtime parameter
		List<T> result= new ArrayList<>();
		for (MutablePair<String,Object[]> rules: this.ruleCommands){
			if (rules.getRight()!=null){
				Object[] params = new Object[rules.right.length];
				for (int i=0;i<rules.getRight().length;i++){
					if (rules.getRight()[i] instanceof IVerificationParam)
						params[i] = ((IVerificationParam)rules.getRight()[i]).getValue(paramContext);
					else
						params[i]=rules.getRight()[i];
				}
				result.add(parameterizedRule(rules.getLeft(), params));
			}else{
				result.add(parameterizedRule(rules.getLeft(),(Object[])null));
			}			
		}
		return result;
	}
	public IVerificationHandler<?> build(){
		return handlerInstance;
	}
	
	public List<MutablePair<String,Object[]>> getRuleCommands(){
		return ruleCommands;
	}
		
	public interface EnumRuleCommand<T>{
		T getRule();
	}
	public interface IRuleFactory{}
}
