package com.msdemo.v2.resource.management.daemon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.core.INameAwareBean;
import com.msdemo.v2.resource.management.zone.ZoneAwareResourceHolder;

public interface IManagedDaemonThread extends Runnable,INameAwareBean{

	static final Logger LOGGER=LoggerFactory.getLogger(IManagedDaemonThread.class);
	
	default void daemonInit(){}
	default boolean isEnabled(){return false;}
	default int getInterval() {return 1000;}
	default int getThreadCount(){return 1;}
	default int getZoneId(){ return ZoneAwareResourceHolder.getBoundZoneId();}
	void daemonExecute();
	
	@Override
	default void run(){		
		try {
			daemonInit();
			daemonExecute();
		} catch (Exception e) {
			LOGGER.error("{}",e);
		}
	}
}
