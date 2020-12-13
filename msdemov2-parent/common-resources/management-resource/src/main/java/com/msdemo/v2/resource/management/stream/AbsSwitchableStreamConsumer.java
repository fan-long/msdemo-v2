package com.msdemo.v2.resource.management.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;

import com.msdemo.v2.common.utils.JsonUtil;
import com.msdemo.v2.resource.management.zone.AbsSwitchableService;
import com.msdemo.v2.resource.management.zone.ZoneAwareResourceHolder;

public abstract class AbsSwitchableStreamConsumer extends AbsSwitchableService {

	private static final Logger logger =LoggerFactory.getLogger(AbsSwitchableStreamConsumer.class);

	@Autowired
	BindingServiceProperties bindingServiceProperties;
	
	@Autowired
	BindingService bindingService;
	
	@Autowired
	SubscribableChannelBindingTargetFactory bindingTargetFactory;
	
	@Autowired
	ConfigurableListableBeanFactory beanFactory;
	
	private String consumerName;
	private  SubscribableChannel inputChannel;

	abstract protected String getTemplateName();
	protected Handler handler = new Handler();
	
	
	@Override
	protected void doActivate() {
		String consumerTemplateName=getTemplateName();
		if (inputChannel==null){
			BindingProperties template =bindingServiceProperties.getBindings()
					.get(consumerTemplateName);
			consumerName=consumerTemplateName;
			if (this.getZoneId()>0){
				BindingProperties bindingProperties =JsonUtil.readValue(JsonUtil.writeValue(template),BindingProperties.class);		
				consumerName=ZoneAwareResourceHolder.getStreamingKey(template.getDestination(),getZoneName());
				bindingProperties.setDestination(consumerName);
				bindingServiceProperties.getBindings().put(consumerName, bindingProperties);
			}
			inputChannel = (SubscribableChannel) bindingTargetFactory.createInput(consumerName);
			String beanName=consumerName.concat("_input");
			beanFactory.registerSingleton(beanName, inputChannel);
			inputChannel = (SubscribableChannel) beanFactory.initializeBean(inputChannel, beanName);
		}
		
		bindingService.bindConsumer(inputChannel,consumerName);
		inputChannel.subscribe(handler);
		logger.warn("channel {} activated",consumerName);

	}
	
	@Override
	protected void doDeactive(){
		if (inputChannel!=null){
			inputChannel.unsubscribe(handler);
			bindingService.unbindConsumers(consumerName);
			logger.warn("channel {} deactived",consumerName);
		}
	}

	abstract protected void onMessage(MessageHeaders headers,String msg);
	
	class Handler implements MessageHandler{

		@Override
		public void handleMessage(Message<?> msg) throws MessagingException {
			ZoneAwareResourceHolder.bindZoneId(getZoneId());
//			String className=msg.getHeaders()
//					.get(AsyncJournalConfiguration.HEADER_CLASS_NAME).toString();
			onMessage(msg.getHeaders(),new String((byte[])msg.getPayload()));	
		}
	}
}
