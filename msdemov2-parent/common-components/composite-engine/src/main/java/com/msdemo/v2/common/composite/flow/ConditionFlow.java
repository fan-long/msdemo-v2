package com.msdemo.v2.common.composite.flow;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.param.ParamMapping;

public class ConditionFlow extends AbstractFlow {

	private static Logger logger =LoggerFactory.getLogger(ConditionFlow.class);
	
	Map<String,AbstractFlow> condMap= new LinkedHashMap<>();
//	Map<String,String> exceptionMap= new LinkedHashMap<>();
	
	AbstractFlow defaultFlow;
	String name;
	boolean breakOnMatch=true;
	String mergeName;
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	public void verify(ProcessDefinition definition,boolean refreshFlag){
		condMap.forEach( (cond,flow)-> flow.verify(definition,refreshFlag));
		if (defaultFlow !=null) defaultFlow.verify(definition, refreshFlag);
	}
	
	@Override
	protected void doExecute(CompositionContext context) throws Exception{
		boolean isMatch=false;
		for (String cond: condMap.keySet()){
			try {
				if (ParamMapping.parser.parseExpression(cond)
						.getValue(context,boolean.class)){
					logger.debug("({}) matched",cond);
					isMatch=true;
					context.put(this.getName(), true);
					condMap.get(cond).execute(context);
					if (breakOnMatch)
						break;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		};
		if (!isMatch){
			if (defaultFlow!=null){
				defaultFlow.execute(context);
				context.put(this.getName(),true);
			}else{
				context.put(this.getName(), false);
			}			
		}
	}
	
	public static class Builder extends AbstractFlowBuilder<ConditionFlow,Builder>{

		@Override
		ConditionFlow init() {
			return new ConditionFlow();
		}
		
		public Builder on(String condEL, AbstractFlow flow){
			getFlow().condMap.put(condEL,flow);
			return this;
		}
		
//		public Builder onException(String condEL, String msg){
//			getFlow().exceptionMap.put(condEL,msg);
//			return this;
//		}
		
		public Builder noneMatch(AbstractFlow flow){
			getFlow().defaultFlow=flow;
			return this;
		}
		public Builder breakOnMatch(boolean breakFlag){
			getFlow().breakOnMatch=breakFlag;
			return this;
		}
		@Override
		public ConditionFlow build(){
			return super.build();
		} 
	}
	
	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		sb.append("<breakOnMatch>").append(breakOnMatch).append("</breakOnMatch>");
		if (condMap.size()>0){
			sb.append("<conditions>");
			for(String cond:condMap.keySet()){
				sb.append("<on>")
					.append("<condEL>").append(cond).append("</condEL>")
					.append(condMap.get(cond).toXml())
					.append("</on>");
			}
			if (defaultFlow!=null)
				sb.append("<noneMatch>").append(defaultFlow.toXml()).append("</noneMatch>");
			sb.append("</conditions>");
		}
		sb.append("</conditionFlow>").insert(0, "<conditionFlow>");
		return sb;
	}

	
}
