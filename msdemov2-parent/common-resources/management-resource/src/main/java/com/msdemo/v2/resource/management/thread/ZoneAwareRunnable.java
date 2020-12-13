package com.msdemo.v2.resource.management.thread;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.resource.management.daemon.DaemonThreadManager;
import com.msdemo.v2.resource.management.daemon.IManagedDaemonThread;
import com.msdemo.v2.resource.management.zone.ZoneAwareResourceHolder;

public class ZoneAwareRunnable implements Runnable{
	private static final Logger logger =LoggerFactory.getLogger(ZoneAwareRunnable.class);
	private final Runnable delegate;
	private final int zoneId;
	private final int interval;
	private final DaemonThreadManager daemonManager;
	public ZoneAwareRunnable(Runnable runnable, int zoneId){
		this.delegate=runnable;
		this.zoneId=zoneId;
		this.daemonManager=null;
		this.interval=0;
	}
	public ZoneAwareRunnable(IManagedDaemonThread runnable, int zoneId, DaemonThreadManager daemonManager){
		this.delegate=runnable;
		this.zoneId=zoneId;
		this.interval=runnable.getInterval();
		this.daemonManager=daemonManager;
		logger.debug("create {} on {}",runnable.name(), this.zoneId);
	}
	@Override
	public void run() {
		ZoneAwareResourceHolder.bindZoneId(zoneId);	
		if (daemonManager==null)
			this.delegate.run();
		else{
			if (daemonManager.isActive() && ((IManagedDaemonThread)delegate).isEnabled()){
				this.delegate.run();
				daemonManager.getExecutor().schedule(this, interval, TimeUnit.MILLISECONDS);
			}else{
				IManagedDaemonThread thread=(IManagedDaemonThread)delegate; 
				logger.warn("{} completed on {}, active: {}, enable: {}",thread.name(),
						 thread.getZoneId(),daemonManager.isActive(),thread.isEnabled());
			}
		}
	}
}
