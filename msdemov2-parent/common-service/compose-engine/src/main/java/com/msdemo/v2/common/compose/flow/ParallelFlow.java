package com.msdemo.v2.common.compose.flow;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import com.msdemo.v2.common.compose.ProcessFlowContext;


public class ParallelFlow extends AbstractFlow {
	private List<AbstractFlow> flowList = new ArrayList<>();
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	//TODO: replace parallelStream with SimpleAsyncTaskExecutor
//	private SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		
	@Override
	public void verify(){
		flowList.forEach(flow-> flow.verify());
	}
	
	@Override
	public void execute(ProcessFlowContext context) throws Exception{
		flowList.parallelStream().forEach( flow ->{
			try {
				flow.execute(context);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		});
	}
	
	public static class Builder extends AbstractFlow.FlowBuilder<ParallelFlow,Builder>{

		@Override
		ParallelFlow init() {
			return new ParallelFlow();
		}
		
		@Deprecated
		@Override
		public Builder beanName(String beanName){
			throw new RuntimeException("property: beanName not allowed for ParallelFlow");
		} 
		
		@Deprecated
		@Override
		public Builder bean(Object bean){
			throw new RuntimeException("property: bean not allowed for ParallelFlow");
		} 
		
		@Deprecated
		@Override
		public Builder method(String methodName){
			throw new RuntimeException("property: method not allowed for ParallelFlow");
		}
		
		public Builder addFlow(AbstractFlow flow){
			getFlow().flowList.add(flow);
			return this;
		}
		
		@Override
		public ParallelFlow build(){
			Assert.isNull(getFlow().invoker.bean, "bean of flow not allowed");
			Assert.isNull(getFlow().invoker.method, "method of flow bean not allowed");
			return super.build();
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
