package com.msdemo.v2.common.compose.flow;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

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
				.getValue(context,IVerificationAware.class),context);
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
				List<MutablePair<String,Object[]>> rules=handler.getBuilder().getRuleCommands();
				for (MutablePair<String,Object[]> rule:rules){
					sb.append("<rule command=\"").append(rule.getKey()).append("\"");
					if (rule.getValue()!=null)
						sb.append(" parameters=\"").append(
								StringUtils.join(rule.getValue(),XmlDefinitionHelper.DELIMITER))
							.append("\"");
					sb.append("/>");		
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
