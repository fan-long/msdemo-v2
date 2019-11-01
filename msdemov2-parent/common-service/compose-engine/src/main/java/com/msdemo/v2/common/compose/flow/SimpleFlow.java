package com.msdemo.v2.common.compose.flow;

import org.springframework.util.Assert;

import com.msdemo.v2.common.compose.ProcessFlowContext;

public class SimpleFlow extends AbstractInvokerFlow {

	//support for combined flow chain
	AbstractFlow nextFlow=null;
	
	public void verify(){
		super.verify();
		if (nextFlow!=null)	nextFlow.verify();
	}
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	public void execute(ProcessFlowContext context) throws Exception{
		super.execute(context);
		if (nextFlow !=null){
			nextFlow.execute(context);
		}		
	}
	
	public static class Builder extends AbstractInvokerFlow.FlowBuilder<SimpleFlow,Builder>{

		@Override
		SimpleFlow init() {
			return new SimpleFlow();
		}
		
		public Builder next(AbstractFlow flow){
			getFlow().nextFlow=flow;
			return this;
		}
		
		@Override
		public SimpleFlow build(){
			if (getFlow().invoker.bean==null)
				Assert.isTrue(getFlow().beanName!=null || getFlow().className!=null
					, "either beanName or className is required");
			Assert.notNull(getFlow().methodName, "methodName is required");
			return super.build();
		}
	}
	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		if (nextFlow!=null)
			sb.append("<nextFlow>").append(nextFlow.toXml()).append("</nextFlow>");
		sb.append("</simpleFlow>").insert(0, "<simpleFlow>");
		return sb;
	}
}
