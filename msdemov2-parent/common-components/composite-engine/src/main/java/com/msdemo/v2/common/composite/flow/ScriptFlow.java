package com.msdemo.v2.common.composite.flow;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.CompositionFactory;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.resource.management.SpringContextHolder;
import com.msdemo.v2.resource.script.spi.IScriptService;

/**
 * do NOT support remote transactional invocation, only used for local
 * @author LONGFAN
 *
 */
public class ScriptFlow extends AbstractFlow{

	private String content;
	private IScriptService service;
	private String serviceClass;
	private String scriptId;
	private boolean shared;
	private boolean transactional;
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	@Override
	protected void doExecute(CompositionContext context) throws Exception {
		//TODO: flag to determine remote/local invocation, required by DTX stage compensation
		super.beforeInvoke(context, null);
		(service==null?context.getScriptService():service).execute(scriptId,context);
	}

	@Override
	public void verify(ProcessDefinition definition,boolean refreshFlag) {
		if (shared){
			service = CompositionFactory.getDefaultScriptService();
			scriptId=this.name;
		}else{
			if (service==null){
				if (StringUtils.isBlank(serviceClass)){
					service=definition.getScriptService();
				}else{
					service= (IScriptService) SpringContextHolder.getSpringBeanByType(serviceClass);		
					if(service ==null)
						throw new RuntimeException(serviceClass+" is undefined in Spring context");
				}
			}
			scriptId=String.format(AbstractInvokerFlow.ScriptKeyPattern,
					definition.getName(),this.name);
		}
		if (!refreshFlag)
			service.load(scriptId,content);
		else
			service.refresh(scriptId,content);	
	}


	public static class Builder extends AbstractFlowBuilder<ScriptFlow,Builder>{

		@Override
		ScriptFlow init() {
			return new ScriptFlow();
		}
		public Builder content(String scriptContent){
			getFlow().content=scriptContent;
			return this;
		}
		public Builder service(IScriptService service){
			getFlow().service=service;
			getFlow().serviceClass=SpringContextHolder.getSpringBeanClassName(service);
			return this;
		}
		public Builder service(String serviceClass){
			getFlow().serviceClass=serviceClass;
			return this;
		}
		public Builder shared(){
			getFlow().shared=true;
			return this;
		}
		public Builder transactional(){
			getFlow().transactional=true;
			getFlow().enableJournal=true;
			return this;
		}
		@Override
		public ScriptFlow build(){
			return super.build();
		} 
	}
	
	public StringBuilder toXml(){
		StringBuilder sb= super.toXml();
		if (shared){
			sb.append("<shared>true</shared>");
		}
		if (transactional){
			sb.append("<transactional>true</transactional>");
		}
		if (StringUtils.isNotBlank(content))
			sb.append("<content>").append(content).append("</content>");
		if (StringUtils.isNotBlank(serviceClass))
			sb.append("<service>").append(serviceClass).append("</service>");
				
		sb.append("</scriptFlow>").insert(0, "<scriptFlow>");
		return sb;
	}

	public boolean isTransactional() {
		return transactional;
	}

	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}

}
