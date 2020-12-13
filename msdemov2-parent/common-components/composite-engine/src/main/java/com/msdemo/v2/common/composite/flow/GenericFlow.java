package com.msdemo.v2.common.composite.flow;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.CompositionFactory;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.composite.spi.IMappingWrapper;

public class GenericFlow extends AbstractFlow {

	String domain;
	String endpoint;
	
	ParamMapping mapping;
	boolean useScript;
	IMappingWrapper wrapper;
	
	@Override
	protected void doExecute(CompositionContext context) throws Exception {
		Object[] args=wrapper.wrap(context);
		super.beforeInvoke(context, args);		
		Object result = CompositionFactory.getDefaultGenericExecutor()
				.invoke(domain,endpoint,args);
		context.put(this.name, result);
		if (StringUtils.isNotEmpty(this.mergeName))
			context.put(mergeName, result);
	}

	@Override
	public void verify(ProcessDefinition definition, boolean refreshFlag) {
		if (useScript){
			String scriptName=String.format(AbstractInvokerFlow.ScriptKeyPattern, definition.getName(),this.name);
			if (refreshFlag)
				wrapper=(IMappingWrapper) definition.getScriptService().refresh(scriptName, null);
			else
				wrapper= (IMappingWrapper) definition.getScriptService().load(scriptName, null);
		}

	}
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	public static class Builder extends AbstractFlowBuilder<GenericFlow, Builder> {
		
		@Override
		GenericFlow init() {
			GenericFlow flow= new GenericFlow();
			flow.enableJournal=true;
			return flow;
		}
		
		public Builder domain(String domain) {
			getFlow().domain=domain;
			return this;
		}

		public Builder endpoint(String endpoint) {
			getFlow().endpoint=endpoint;
			return this;
		}

		public Builder mapping(ParamMapping mapping) {
			getFlow().mapping=mapping;
			return  this;
		}

		public Builder wrapper(IMappingWrapper wrapper){
			getFlow().wrapper=wrapper;
			return this;
		}
		
		public Builder useScript(){
			getFlow().useScript=true;
			return this;
		}
		
		public GenericFlow build() {
			Assert.notNull(getFlow().domain, "domain is required");
			Assert.notNull(getFlow().endpoint, "endpoint is required");
			Assert.isTrue(getFlow().mapping!=null || getFlow().wrapper!=null || getFlow().useScript, 
					"one of 'mapping' or 'wrapper' or 'useScript' should be provided");
			Assert.notNull(getFlow().name, "flow name is required");
			return (GenericFlow) super.build();
		}		
	}
	
	public String getDomain(){return this.domain;}
	public String getEndpoint(){return this.endpoint;}
	
	public StringBuilder toXml(){
		StringBuilder sb = super.toXml();
		sb.append("<domain>").append(domain).append("</domain>");
		sb.append("<endpoint>").append(endpoint).append("</endpoint>");
		
		if (mapping != null) {
			sb.append(mapping.toXml());
		}else{
			sb.append("<mappingWrapper ");	
			if (useScript){
				sb.append("useScript=\"true\"/>");
			}else
				sb.append("class=\"").append(wrapper.getClass().getName()).append("\"/>");
		}
		return 	sb.append("</genericFlow>").insert(0, "<genericFlow>");
	}
}
