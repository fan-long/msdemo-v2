package com.msdemo.v2.resource.management.thread;

import java.util.concurrent.Callable;

import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.context.TransContext.Context;
import com.msdemo.v2.resource.management.zone.ZoneAwareResourceHolder;

class TransContextAwareCallable<V> implements Callable<V>{
	private final Context parentContext;
	private final Callable<V> delegate;
	public TransContextAwareCallable(Context context,Callable<V> callable){
		this.parentContext=context;
		this.delegate=callable;			
	}
	@Override
	public V call() throws Exception{
		TransContext.replace(parentContext);
		ZoneAwareResourceHolder.applyZoneTrace();
		return this.delegate.call();			
	}
}