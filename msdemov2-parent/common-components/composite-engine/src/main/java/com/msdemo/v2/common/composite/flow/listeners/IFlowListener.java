package com.msdemo.v2.common.composite.flow.listeners;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.flow.AbstractFlow;

public interface IFlowListener {
	
	void beforeFlow(AbstractFlow flow, CompositionContext context);

	default void beforeInvoke(AbstractFlow flow, CompositionContext context, Object[] args){};
	
	void afterFlow(AbstractFlow flow, CompositionContext context);

}
