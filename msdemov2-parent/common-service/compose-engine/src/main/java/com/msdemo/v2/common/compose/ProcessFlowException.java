package com.msdemo.v2.common.compose;

public class ProcessFlowException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5749592238135657716L;
	private ProcessFlowContext context;
	
	public ProcessFlowException(ProcessFlowContext context,Throwable e,String msg){
		super(msg,e);
		this.context=context;
	}

	public ProcessFlowContext getContext() {
		return context;
	}

}
