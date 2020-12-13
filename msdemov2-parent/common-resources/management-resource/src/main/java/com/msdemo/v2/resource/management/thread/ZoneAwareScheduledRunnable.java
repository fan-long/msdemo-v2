package com.msdemo.v2.resource.management.thread;

public class ZoneAwareScheduledRunnable implements Runnable {

	private Runnable delegate;
	
	public ZoneAwareScheduledRunnable(Runnable r){
		this.delegate=r;
	}
	@Override
	public void run() {
		this.delegate.run();
	}

}
