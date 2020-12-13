package com.msdemo.v2.common.composite.flow;

import java.util.ArrayList;
import java.util.List;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;


public class ParallelFlow extends AbstractFlow {
	private List<AbstractFlow> flowList = new ArrayList<>();
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	//TODO: replace parallelStream with SimpleAsyncTaskExecutor
//	private SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		
	@Override
	public void verify(ProcessDefinition definition,boolean refreshFlag){
		flowList.forEach(flow-> flow.verify(definition,refreshFlag));
	}
	
	@Override
	protected void doExecute(CompositionContext context) throws Exception{
		flowList.parallelStream().forEach( flow ->{
			try {
				flow.execute(context);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		});
		context.put(name, true);
	}
	
	public static class Builder extends AbstractFlowBuilder<ParallelFlow,Builder>{

		@Override
		ParallelFlow init() {
			return new ParallelFlow();
		}
		
//		@Deprecated
//		@Override
//		public Builder beanName(String beanName){
//			throw new RuntimeException("property: beanName not allowed for ParallelFlow");
//		} 
//		
//		@Deprecated
//		@Override
//		public Builder bean(Object bean){
//			throw new RuntimeException("property: bean not allowed for ParallelFlow");
//		} 
//		
//		@Deprecated
//		@Override
//		public Builder method(String methodName){
//			throw new RuntimeException("property: method not allowed for ParallelFlow");
//		}
		
		public Builder addFlow(AbstractFlow flow){
			getFlow().flowList.add(flow);
			return this;
		}
		
	}

	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		sb.append("<flowList>");
		for(AbstractFlow flow:flowList){
			sb.append(flow.toXml());
		}
		sb.append("</flowList>");
		sb.append("</parallelFlow>").insert(0, "<parallelFlow>");
		return sb;
	}
	
}
