package com.msdemo.v2.common.composite.flow;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.verification.VerificationChain;
import com.msdemo.v2.common.verification.chain.IVerificationAware;
import com.msdemo.v2.common.verification.chain.IVerificationHandler;
import com.msdemo.v2.common.verification.chain.IVerificationParam;

public class VerificationFlow extends AbstractFlow{
	
	VerificationChain chain;

	String data;

	@Override
	protected void doExecute(CompositionContext context) throws Exception {
		chain.verify(ParamMapping.parser.parseExpression(data)
				.getValue(context,IVerificationAware.class),context);
		context.put(name, true);
	}

	@Override
	public void verify(ProcessDefinition definition,boolean refreshFlag) {
		if(StringUtils.isEmpty(data))
			throw new RuntimeException(" data is required");		
	}
	
	public static Builder builder(){		
		return new FlowFactory<Builder>().get(Builder.class);
	}
	
	public static class Builder extends AbstractFlowBuilder<VerificationFlow,Builder>{

		VerificationChain.Builder builder=VerificationChain.builder();
		@Override
		VerificationFlow init() {
			return new VerificationFlow();
		}
		
		public Builder handler(IVerificationHandler<?> handler){
			builder.handle(handler);
			return this;
		}
		
		public Builder data(String dataEL){
			getFlow().data=dataEL;
			return this;
		}
		
		@Override
		public VerificationFlow build(){
			getFlow().chain=builder.build();
			return getFlow();
		} 
	}

	@Override
	public StringBuilder toXml() {
		StringBuilder sb= super.toXml();
		if (chain.getHandler().size()>0){
			sb.append("<handlers>");
			for(IVerificationHandler<?> handler:chain.getHandler()){
				sb.append("<handler class=\"").append(handler.getClass().getName()).append("\">");
				List<MutablePair<String,Object[]>> rules=handler.getBuilder().getRuleCommands();
				for (MutablePair<String,Object[]> rule:rules){
					sb.append("<rule command=\"").append(rule.getKey()).append("\">");
					if (rule.getValue()!=null){
						for (Object param:rule.getValue()){
							if (param instanceof IVerificationParam){
								sb.append("<argument class=\"").append(
										param.getClass().getName()).append("\" source=\"")
								.append(((IVerificationParam) param).getParam()).append("\"/>");
							}else{
								sb.append("<argument source=\"").append(param).append("\"/>");
							}
						}
					}
					sb.append("</rule>");		
				}
				sb.append("</handler>");				
			}
			sb.append("</handlers>");
		}
		sb.append("<data source=\"").append(data).append("\"/>");
		sb.append("</verificationFlow>").insert(0, "<verificationFlow>");
		return sb;
	}

}
