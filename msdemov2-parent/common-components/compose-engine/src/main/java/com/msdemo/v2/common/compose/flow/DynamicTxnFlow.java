package com.msdemo.v2.common.compose.flow;

import java.util.LinkedHashMap;
import java.util.Map;

import com.msdemo.v2.common.compose.ProcessFlow.TxnType;
import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.param.ParamMapping;

public class DynamicTxnFlow extends AbstractFlow {

	Map<String,TxnType> condMap= new LinkedHashMap<>(4);
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	public void execute(ProcessFlowContext context) throws Exception{
		if(!TxnType.Dynamic.equals(context.getTxnType())){
			throw new RuntimeException("initial transaction type should be Dynamic");
		}
		for (String cond: condMap.keySet()){
			if (ParamMapping.parser.parseExpression(cond)
				.getValue(context,boolean.class)){
				TxnType newTxnType=condMap.get(cond);
				context.setTxnType(newTxnType);
				context.put(name, newTxnType);
				break;
			}
		}
	}
	public static class Builder extends AbstractFlowBuilder<DynamicTxnFlow,Builder>{

		@Override
		DynamicTxnFlow init() {
			return new DynamicTxnFlow();
		}
		public Builder on(String condEL, TxnType newTxnType){
			getFlow().condMap.put(condEL,newTxnType);
			return this;
		}
	}
	
	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		if (condMap.size()>0){
			sb.append("<conditions>");
			for(String cond:condMap.keySet()){
				sb.append("<on>")
				.append("<condEL>").append(cond).append("</condEL>")
				.append("<txnType>").append(condMap.get(cond)).append("</txnType>")
				.append("</on>");
			}
			sb.append("</conditions>");
		}
		sb.append("</dynamicTxnFlow>").insert(0, "<dynamicTxnFlow>");
		return sb;
	}

	@Override
	public void verify(boolean refreshFlag) {
		
	}
}
