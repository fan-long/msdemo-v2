package com.msdemo.v2.resource.management.thread;

import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.context.TransContext.Context;
import com.msdemo.v2.resource.management.zone.ZoneAwareResourceHolder;

class TransContextAwareRunnable extends ZoneAwareRunnable {
	private final Context parentContext;
	public TransContextAwareRunnable(Context context,Runnable task){
		super(task,ZoneAwareResourceHolder.getBoundZoneId());
		this.parentContext=context;
	}

	@Override
	public void run() {
		TransContext.replace(parentContext);
		super.run();
//		logger.debug("current bound unit: {}, trace: {}, context: {}, delegate: {}",
//				ZoneSwitchUtil.getBoundZoneId(),
//				parentContext, this.delegate);
	}
}
