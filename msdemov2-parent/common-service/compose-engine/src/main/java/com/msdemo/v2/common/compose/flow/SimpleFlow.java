package com.msdemo.v2.common.compose.flow;

import org.springframework.util.Assert;

import com.msdemo.v2.common.compose.ProcessFlowContext;

public class SimpleFlow extends AbstractFlow {

	//support for combined flow chain
	SimpleFlow nextFlow=null;
	
	public void verify(){
		super.verify();
		if (nextFlow!=null)	nextFlow.verify();
	}
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	public Object execute(ProcessFlowContext context) throws Exception{
		Object result=super.execute(context);
		if (nextFlow !=null){
			return nextFlow.execute(context);
		}else{
			return result;
		}
		
	}
	
	public static class Builder extends AbstractFlow.FlowBuilder<SimpleFlow,Builder>{

		@Override
		SimpleFlow init() {
			return new SimpleFlow();
		}
		
		public Builder next(SimpleFlow flow){
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
}
