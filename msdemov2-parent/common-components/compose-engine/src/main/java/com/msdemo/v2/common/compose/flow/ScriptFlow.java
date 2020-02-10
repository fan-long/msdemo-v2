package com.msdemo.v2.common.compose.flow;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.ProcessFlowFactory;
import com.msdemo.v2.common.core.IScriptService;

public class ScriptFlow extends AbstractFlow{

	private String key;
	private String content;
	private IScriptService service;
	private boolean defaultFlag;
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	public void execute(ProcessFlowContext context) throws Exception {
		if (StringUtils.isNotEmpty(key))
			service.execute(key,context);
		else
			service.executeScript(content,context);
	}

	@Override
	public void verify(boolean refreshFlag) {
		if (service==null){
			service= (IScriptService) ProcessFlowFactory.getSpringBeanByType(IScriptService.class.getSimpleName());		
			if(service ==null)
				throw new RuntimeException("IScriptService is undefined in Spring context");			
			defaultFlag =true;
		}
		if(StringUtils.isNoneEmpty(key,content)){
			//service.cacheScript(key,content);
			throw new RuntimeException("key should be auto generated if content provided");
		}else if (StringUtils.isNotEmpty(key)){
			if (!refreshFlag)
				service.load(key);
			else
				service.reload(key);
		}else if (StringUtils.isNotEmpty(content)){
			key=String.valueOf(content.hashCode());
			service.cacheScript(key,content);	
		}
		else
			throw new RuntimeException("script key or content is required");

	}


	public static class Builder extends AbstractFlowBuilder<ScriptFlow,Builder>{

		@Override
		ScriptFlow init() {
			return new ScriptFlow();
		}
		
		public Builder key(String scriptKey){
			getFlow().key=scriptKey;
			return this;
		}
		public Builder content(String scriptContent){
			getFlow().content=scriptContent;
			return this;
		}
		public Builder service(IScriptService service){
			getFlow().service=service;
			getFlow().defaultFlag=false;
			return this;
		}
		public Builder service(String serviceClass){
			if (StringUtils.isNotEmpty(serviceClass))
				getFlow().service=(IScriptService) ProcessFlowFactory.getSpringBeanByType(serviceClass);
			return this;
		}
		
		@Override
		public ScriptFlow build(){
			return super.build();
		} 
	}
	
	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		if (StringUtils.isNotEmpty(key))
			sb.append("<key>").append(key).append("</key>");
		else
			sb.append("<content>").append(content).append("</content>");
		if (!defaultFlag)
			sb.append("<service>").append(service.getClass().getName()).append("</service>");
		
		sb.append("</scriptFlow>").insert(0, "<scriptFlow>");
		return sb;
	}

	

}
