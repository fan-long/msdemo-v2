package com.msdemo.v2.common.compose.flow;

import org.springframework.util.Assert;

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
		int superTagLength="simpleFlow".length();
		StringBuilder sb= super.toXml();
		sb.replace(0, superTagLength+2, "").replace(sb.length()-(superTagLength+3), sb.length(), "");
		sb.append("</asyncFlow>").insert(0, "<asyncFlow>");
		return sb;
	}
}
