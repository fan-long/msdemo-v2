package com.msdemo.v2.resource.management.zone;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class ZoneStatusManager implements ApplicationListener<ContextRefreshedEvent>{

	static volatile boolean activeFlag= true;

	private static Logger logger =LoggerFactory.getLogger(ZoneStatusManager.class);

	private ApplicationContext context;
	@Override
    public void onApplicationEvent(ContextRefreshedEvent evt) {
        if (evt.getApplicationContext().getParent().getId().equals("bootstrap")) {
			logger.info("*** start to activate switchable services");
			this.context=evt.getApplicationContext();
			this.switchStatus();
			logger.info("*** end to activate switchable services");			
        }
	}
	
	protected void switchStatus(){
		Map<String,IManagedSwitchableService> beans=context.getBeansOfType(IManagedSwitchableService.class);
		if (beans!=null && !beans.isEmpty()){
			for (IManagedSwitchableService service: beans.values()){
				if (service.isActive())
					service.deactive();
				else
					service.activate();
			}
		}
	}
	
	public static boolean isNodeActive(){return activeFlag;}
	
	public static void setNodeActive(boolean flag){ 
		activeFlag=flag;
	}
	
	public String getDataCenterId(){ return "1";}
}
