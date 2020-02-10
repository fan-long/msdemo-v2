package com.msdemo.v2.common.compose.flow;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.param.ParamMapping;

public class ConditionFlow extends AbstractFlow {

	private static Logger logger =LoggerFactory.getLogger(ConditionFlow.class);
	
	Map<String,AbstractFlow> condMap= new LinkedHashMap<>();
	
	String name;
	boolean breakOnMatch=true;
	String mergeName;
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	public void verify(boolean refreshFlag){
		condMap.forEach( (cond,flow)-> flow.verify(refreshFlag));
	}
	
	@Override
	public void execute(ProcessFlowContext context) throws Exception{
		for (String cond: condMap.keySet()){
			try {
				if (ParamMapping.parser.parseExpression(cond)
						.getValue(context,boolean.class)){
					logger.debug("({}) matched",cond);
					condMap.get(cond).execute(context);
					context.put(this.getName(), true);
					if (breakOnMatch)
						break;
				}else{
					context.put(this.getName(), false);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		};
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
			sb.append("</conditions>");
		}
		sb.append("</conditionFlow>").insert(0, "<conditionFlow>");
		return sb;
	}

	
}
