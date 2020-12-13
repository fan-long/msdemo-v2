package com.msdemo.v2.resource.management.stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.MessageChannel;

import com.msdemo.v2.common.utils.JsonUtil;
import com.msdemo.v2.resource.management.zone.AbsSwitchableService;
import com.msdemo.v2.resource.management.zone.ZoneAwareResourceHolder;

public abstract class AbsSwitchableStreamProducer extends AbsSwitchableService {

	@Autowired
	BinderAwareChannelResolver resolver;
		
	@Autowired
	BindingServiceProperties bindingServiceProperties;
	
	private MessageChannel outputChannel;
	
	protected MessageChannel getOutputChannel(){
		return this.outputChannel;
	}
	
	abstract protected String getTemplateName();

	@Override
	protected void doActivate() {
		if (this.outputChannel==null){
			String procuderTemplateName=getTemplateName();
			BindingProperties template =bindingServiceProperties.getBindings()
					.get(procuderTemplateName);
			String procuderName=procuderTemplateName;
			if (this.getZoneId()>0){
				procuderName=ZoneAwareResourceHolder.getStreamingKey(template.getDestination(),getZoneName());		
				BindingProperties bindingProperties =JsonUtil.readValue(JsonUtil.writeValue(template),BindingProperties.class);
				bindingProperties.setDestination(procuderName);
				bindingServiceProperties.getBindings().put(procuderName, bindingProperties);
			}				
			this.outputChannel=resolver.resolveDestination(procuderName);
		}

	}
	@Override
	public void doDeactive() {		
	}
}
