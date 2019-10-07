package com.msdemo.v2.common.compose.flow;

import com.msdemo.v2.common.compose.ProcessFlow.TxnType;

import java.util.LinkedHashMap;
import java.util.Map;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.param.ParamMapping;

public class DynamicTxnFlow extends ConditionFlow {

	Map<String,TxnType> condMap= new LinkedHashMap<>(3);
	
	public static Builder dynamicTxnBuilder(){		
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
	public static class Builder extends AbstractFlow.FlowBuilder<DynamicTxnFlow,Builder>{

		@Override
		DynamicTxnFlow init() {
			return new DynamicTxnFlow();
		}
		public Builder on(String condEL, TxnType newTxnType){
			getFlow().condMap.put(condEL,newTxnType);
			return this;
		}
	}
}
