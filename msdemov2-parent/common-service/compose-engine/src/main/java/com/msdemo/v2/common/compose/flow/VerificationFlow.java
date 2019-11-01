package com.msdemo.v2.common.compose.flow;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.handler.XmlDefinitionHelper;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.core.IVerificationAware;
import com.msdemo.v2.common.verification.IVerificationHandler;
import com.msdemo.v2.common.verification.VerificationChain;

public class VerificationFlow extends AbstractFlow{
	
	VerificationChain chain;

	String data;

	@Override
	public void execute(ProcessFlowContext context) throws Exception {
		chain.verify(ParamMapping.parser.parseExpression(data)
				.getValue(context,IVerificationAware.class));
	}

	@Override
	public void verify() {
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
				HashMap<String,String[]> rules=handler.getBuilder().getRuleCommands();
				for (String command:rules.keySet()){
					sb.append("<rule command=\"").append(command).append("\"");
					if (rules.get(command)!=null)
						sb.append(" parameters=\"").append(
								StringUtils.join(rules.get(command),XmlDefinitionHelper.DELIMITER))
							.append("\"");
					sb.append("/>");		
				}
				sb.append("</handler>");				
			}
			sb.append("</handlers>");
		}
		sb.append("<data>").append("</data>");
		sb.append("</verificationFlow>").insert(0, "<verificationFlow>");
		return sb;
	}

}
