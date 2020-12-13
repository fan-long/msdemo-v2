package com.msdemo.v2.common.composite.flow;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;

public class AsyncFlow extends AbstractInvokerFlow {

	//support for combined flow chain
	AbstractFlow nextFlow=null;
		
	public static AsyncBuilder builder(){		
		return new FlowFactory<AsyncBuilder>().get(AsyncBuilder.class);
	}
			
	public void verify(ProcessDefinition definition,boolean refreshFlag){
		super.verify(definition,refreshFlag);
		if (nextFlow!=null)	nextFlow.verify(definition,refreshFlag);
	}
	
	@Override
	protected void doExecute(CompositionContext context) throws Exception{
		AsyncFlow flow = this;
		new Thread( () ->{
			CompositionContext asyncContext= (CompositionContext) context.clone();
			try {
				super.doExecute(asyncContext);				
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
		
	}
	
	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		sb.append("</asyncFlow>").insert(0, "<asyncFlow>");
		return sb;
	}
}
