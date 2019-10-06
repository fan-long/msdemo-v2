package com.msdemo.v2.common.compose.flow;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.param.ParamMapping;

public class ConditionFlow extends AbstractFlow {

	Map<String,AbstractFlow> condMap= new LinkedHashMap<>();
		
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
		
	@Override
	public void verify(){
		condMap.forEach( (cond,flow)-> flow.verify());
	}
	
	@Override
	public Object execute(ProcessFlowContext context) throws Exception{
		for (String cond: condMap.keySet()){
			try {
				if (ParamMapping.parser.parseExpression(cond)
						.getValue(context,boolean.class)){
					logger.debug("({}) matched",cond);
					context.put(this.getName(), condMap.get(cond).execute(context));
					break;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		};
		return context;
	}
	
	public static class Builder extends AbstractFlow.FlowBuilder<ConditionFlow,Builder>{

		@Override
		ConditionFlow init() {
			return new ConditionFlow();
		}
		
		@Deprecated
		@Override
		public Builder beanName(String beanName){
			throw new RuntimeException("property: beanName not allowed for ConditionFlow");
		} 
		
		@Deprecated
		@Override
		public Builder bean(Object bean){
			throw new RuntimeException("property: bean not allowed for ConditionFlow");
		} 
		
		@Deprecated
		@Override
		public Builder method(String methodName){
			throw new RuntimeException("property: method not allowed for ConditionFlow");
		}
		
		public Builder on(String condEL, AbstractFlow flow){
			getFlow().condMap.put(condEL,flow);
			return this;
		}
		
		@Override
		public ConditionFlow build(){
			Assert.isNull(getFlow().invoker.bean, "bean of flow not allowed");
			Assert.isNull(getFlow().invoker.method, "method of flow bean not allowed");
			return super.build();
		} 
	}
}
