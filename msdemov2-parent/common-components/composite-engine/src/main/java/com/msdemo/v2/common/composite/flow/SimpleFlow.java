package com.msdemo.v2.common.composite.flow;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;

public class SimpleFlow extends AbstractInvokerFlow {

	//support for combined flow chain
	AbstractFlow nextFlow=null;
	
	public void verify(ProcessDefinition definition,boolean refreshFlag){
		super.verify(definition,refreshFlag);
		if (nextFlow!=null)	nextFlow.verify(definition,refreshFlag);

	}
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	protected void doExecute(CompositionContext context) throws Exception{
		super.doExecute(context);
		if (nextFlow !=null){
			nextFlow.execute(context);
		}		
	}
	
	public static class Builder extends AbstractInvokerFlow.FlowBuilder<SimpleFlow,Builder>{

		@Override
		SimpleFlow init() {
			SimpleFlow flow =new SimpleFlow();
			flow.enableJournal=true;
			return flow;
		}
		
		public Builder next(AbstractFlow flow){
			getFlow().nextFlow=flow;
			return this;
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
