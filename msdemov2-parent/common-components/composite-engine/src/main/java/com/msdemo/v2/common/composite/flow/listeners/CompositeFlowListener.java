package com.msdemo.v2.common.composite.flow.listeners;

import java.util.ArrayList;
import java.util.List;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.flow.AbstractFlow;

public class CompositeFlowListener implements IFlowListener {

	private List<IFlowListener> delegates = new ArrayList<>();
	
	public CompositeFlowListener addListener(IFlowListener listener){
		delegates.add(listener);
		return this;
	}
	
	public List<IFlowListener> getDelegates(){
		return delegates;
	}

	@Override
	public void beforeFlow(AbstractFlow flow, CompositionContext context) {
		for (IFlowListener listener: delegates){
			listener.beforeFlow(flow, context);
		}		
	}
	@Override
	public void beforeInvoke(AbstractFlow flow, CompositionContext context, Object[] args){
		for (IFlowListener listener: delegates){
			listener.beforeInvoke(flow, context,args);
		}
	}
	@Override
	public void afterFlow(AbstractFlow flow, CompositionContext context) {
		for (int i=delegates.size()-1;i>=0;i--){
			delegates.get(i).afterFlow(flow, context);
		}
		
	}
}
