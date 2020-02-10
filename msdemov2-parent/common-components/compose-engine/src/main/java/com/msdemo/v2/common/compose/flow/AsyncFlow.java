package com.msdemo.v2.common.compose.flow;

import org.springframework.util.Assert;

import com.msdemo.v2.common.compose.ProcessFlowContext;

public class AsyncFlow extends AbstractInvokerFlow {

	//support for combined flow chain
	AbstractFlow nextFlow=null;
		
	public static AsyncBuilder builder(){		
		return new FlowFactory<AsyncBuilder>().get(AsyncBuilder.class);
	}
			
	public void verify(boolean refreshFlag){
		super.verify();
		if (nextFlow!=null)	nextFlow.verify(refreshFlag);
	}
	
	@Override
	public void execute(ProcessFlowContext context) throws Exception{
		AsyncFlow flow = this;
		new Thread( () ->{
			ProcessFlowContext asyncContext= (ProcessFlowContext) context.clone();
			try {
				super.execute(asyncContext);				
			} catch (Exception e) {
				asyncContext.put(flow.name, e.getMessage());
			}			
		}).start();
	}
	
	public static class AsyncBuilder extends AbstractInvokerFlow.FlowBuilder<AsyncFlow,AsyncBuilder>{

		@Override
		AsyncFlow init() {
			return new AsyncFlow();
		}

		public AsyncBuilder next(AbstractFlow flow){
			getFlow().nextFlow=flow;
			return this;
		}
		
		@Override
		public AsyncFlow build(){
			if (getFlow().invoker.bean==null)
				Assert.isTrue(getFlow().beanName!=null || getFlow().className!=null
					, "either beanName or className is required");
			Assert.notNull(getFlow().methodName, "methodName is required");
			return super.build();
		}
	}
	
	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		sb.append("</asyncFlow>").insert(0, "<asyncFlow>");
		return sb;
	}
}
