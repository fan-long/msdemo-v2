package com.msdemo.v2.resource.management.daemon;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.msdemo.v2.resource.management.shutdown.GracefulShutdownEventListener;
import com.msdemo.v2.resource.management.thread.ZoneAwareRunnable;
import com.msdemo.v2.resource.management.zone.AbsSwitchableService;

@Component
public class DaemonThreadManager extends AbsSwitchableService{
	private static final String THREAD_NAME_PREFIX="daemon-";
	private static final Logger logger= LoggerFactory.getLogger(DaemonThreadManager.class);

	@Autowired
	@Nullable
	IManagedDaemonThread[] daemons;
	
	private ScheduledExecutorService executor;
	
	@Override
	protected void doActivate(){
		if (daemons==null) return;		
		if (executor==null){
			int totalThread=0;
			for (IManagedDaemonThread thread: daemons){
				totalThread+= thread.getThreadCount();
			}			AtomicInteger index= new AtomicInteger();
			executor=Executors.newScheduledThreadPool(totalThread, new ThreadFactory(){
				@Override
				public Thread newThread(Runnable r) {
					Thread t= new Thread(r);
					if (StringUtils.isNotBlank(getZoneName())){
						t.setName(THREAD_NAME_PREFIX.concat(getZoneName())
								.concat("#")+index.incrementAndGet());
					}else
						t.setName(THREAD_NAME_PREFIX+index.incrementAndGet());
					return t;
				}
				
			});
			GracefulShutdownEventListener.register(executor);
		}
	
		int index=0;
		for (IManagedDaemonThread thread: daemons){
			for (int i=0;i<thread.getThreadCount();i++){
				executor.execute(new ZoneAwareRunnable(thread,getZoneId(),this));
				logger.info("{}{}#{}, {} thread started by class {}",THREAD_NAME_PREFIX,getZoneName(),
						++index,thread.name(),thread.getClass().getSimpleName());
			}
		}
	}
	
	protected void doDeactive(){
		//do nothing, since ZoneAwareRunnable will check active status of manager
	}

	public ScheduledExecutorService getExecutor(){
		return this.executor;
	}
}
