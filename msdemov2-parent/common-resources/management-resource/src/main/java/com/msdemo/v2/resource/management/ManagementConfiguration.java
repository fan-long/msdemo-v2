package com.msdemo.v2.resource.management;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.resource.management.acl.AdditionalPortCustomizer;
import com.msdemo.v2.resource.management.acl.IPAccessPolicyCustomizer;
import com.msdemo.v2.resource.management.zone.SwitchableBeanPostBuilder;
import com.msdemo.v2.resource.management.zone.ZoneStatusManager;

public class ManagementConfiguration {

	public static final String MANANGEMENT_PREFIX=CommonConstants.CONFIG_ROOT_PREFIX+".management";
	
	@ConditionalOnProperty(value=ManagementConfiguration.MANANGEMENT_PREFIX+".acl", havingValue="true")
	@Configuration
	static class AclDef{
		@Bean
		IPAccessPolicyCustomizer ipAccessPolicy(){
			return new IPAccessPolicyCustomizer();
		}
	} 
		
	@ConditionalOnProperty(value=ManagementConfiguration.MANANGEMENT_PREFIX+".admin-port", havingValue="true")
	@Configuration
	static class AdminPortDef{
		@Bean
		AdditionalPortCustomizer additionalPortCustomizer(){
			return new AdditionalPortCustomizer();
		}
	} 
	
	@Bean
	@ConditionalOnMissingBean(SwitchableBeanPostBuilder.class)
	SwitchableBeanPostBuilder switchableBeanPostBuilder(){
		return new SwitchableBeanPostBuilder();
	}
	
	@Bean
	@ConditionalOnMissingBean(ZoneStatusManager.class)
	ZoneStatusManager switchableBeanManager(){
		return new ZoneStatusManager();
	}
}
