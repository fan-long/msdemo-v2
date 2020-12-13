package com.msdemo.v2.common.composite.chain;

import com.msdemo.v2.common.composite.CompositionContext;

public abstract class AbsTxnInterceptor extends AbsProcessInterceptor{
	
    @Override
    protected CompositionContext finalInvoke(InterceptorModel model){
    	return txnInvoke(model);
    } 
    protected static CompositionContext txnInvoke(InterceptorModel model){
    	model.context= model.context==null?model.definition.executeObj(model.requestDto)
				:model.definition.execute(model.context);
    	return model.context;
    } 
}
