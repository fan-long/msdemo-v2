package com.msdemo.v2.resource.management.zone;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class SwitchableBeanPostBuilder {

	private boolean readyFlag;
	
	public boolean isReady(){
		return readyFlag;
	}
		
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory){
		this.readyFlag=true;
	}
}
