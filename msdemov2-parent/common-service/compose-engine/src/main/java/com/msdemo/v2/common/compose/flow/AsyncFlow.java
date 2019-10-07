package com.msdemo.v2.common.compose.flow;

import com.msdemo.v2.common.compose.ProcessFlowContext;

public class AsyncFlow extends SimpleFlow {

	public static AsyncBuilder asyncBuilder(){		
		return new FlowFactory<AsyncBuilder>().get(AsyncBuilder.class);
	}
	
	@Deprecated
	public static Builder builder(){
		return null;
	}
	
	@Override
	public void execute(ProcessFlowContext context) throws Exception{
		AsyncFlow flow = this;
		new Thread( () ->{
			ProcessFlowContext asyncContext= (ProcessFlowContext) context.clone();
			try {
				super.execute(asyncContext);
				if (flow.nextFlow !=null){
					flow.nextFlow.execute(asyncContext);
				}
			} catch (Exception e) {
				asyncContext.put(flow.name, e.getMessage());
			}			
		}).start();
	}
	
	public static class AsyncBuilder extends AbstractFlow.FlowBuilder<AsyncFlow,AsyncBuilder>{

		@Override
		AsyncFlow init() {
			return new AsyncFlow();
		}
	}
}
