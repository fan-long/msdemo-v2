package com.msdemo.v2.common.composite;

public class CompositionException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5749592238135657716L;
	private CompositionContext context;
	
	public CompositionException(CompositionContext context,Throwable e,String msg){
		super(msg,e);
		this.context=context;
	}

	public CompositionException(String msg){
		super(msg);
	}
	public CompositionContext getContext() {
		return context;
	}

}
