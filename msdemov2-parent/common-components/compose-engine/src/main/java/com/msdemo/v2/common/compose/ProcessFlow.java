package com.msdemo.v2.common.compose;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.msdemo.v2.common.compose.flow.AbstractFlow;
import com.msdemo.v2.common.compose.handler.IExceptionHandler;
import com.msdemo.v2.common.compose.param.INewInstance;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.util.LogUtils;
import com.msdemo.v2.common.util.XmlUtils;

public final class ProcessFlow {
	@JsonIgnore
	private static Logger logger =LoggerFactory.getLogger(ProcessFlow.class);
	
	@JsonProperty
	String name;
	@JsonProperty
	TxnType txnType=TxnType.Non;
	IExceptionHandler exceptionHandler;
	@JsonProperty
	List<AbstractFlow> flowList = new ArrayList<>();
	@JsonProperty
	ParamMapping resultMapping;
	@JsonProperty
	INewInstance resultObject;
	
	public ProcessFlow(String name){
		this.name=name;
	}
	
	public void verify(boolean refreshFlag){
		flowList.forEach( flow ->{
			flow.verify(refreshFlag);
		});
	}
	public ProcessFlowContext execute(ProcessFlowContext context,Object req){
		String flowName=null;
		try{
			long start;
			for (int i=0;i<flowList.size();i++){
				start =System.currentTimeMillis();
				AbstractFlow flow=flowList.get(i);
				flowName=flow.getName();
				//for dynamic transaction re-execution, do NOT execute completed flow
				if (!context.containsKey(flow.getName())){
					flow.execute(context);
					LogUtils.cost(null, start, flowName);
					//if transaction type changed, stop the process and return to ProcessFlowFactory
					if (context.isTxnTypeChanged()) 
						return context;
				}
			};
			//response
			if (resultMapping!=null){
				flowName=INewInstance.USAGE;
				context.setResp(resultObject.newInstance());			
				SpelExpressionParser parser = ParamMapping.parser;
				for (MutablePair<String,String> pair:resultMapping){
					Object value=parser.parseExpression(pair.getRight()).getValue(context);
					parser.parseExpression(pair.getLeft()).setValue(context,value);
				}
			}
		}catch(Exception e){
			this.exceptionHandler.handle(flowName, context, e);
			if (context.getException()!=null) throw context.getException();
		}
		return context;
	}
	public ProcessFlowContext execute(Object req){
//		long start=System.currentTimeMillis();
		ProcessFlowContext context = new ProcessFlowContext(this.name,this.txnType);
		context.setReq(req);
//		try {
			return execute(context,req);
//		} finally{
//			if (!context.isTxnTypeChanged()) //flow would be re-execute, do NOT print time cost 
//				LogUtils.cost(logger, start, name);
//		}
	}

	public enum TxnType{		
		Global, //Global transaction, such as SAGA
		Local, //Local Transaction, such as JDBC, JMS
		Dynamic, //Dynamic transaction, actual transaction type would be determined during process execution
		Prepare, //only used for JVM initialize, run dummy TXN then roll back
		Non; 	//disable transaction management, for inquire TXN
	}
	
	public String toXml(){
		StringBuilder sb =new StringBuilder();
		sb.append("<processFlow name=\"").append(name)
			.append("\" txnType=\"").append(txnType)
			.append("\" exceptionHandler=\"").append(exceptionHandler.getClass().getName()).append("\">")
			.append("<flowList>");
		for (AbstractFlow flow:flowList){
			sb.append(flow.toXml());
		}
		sb.append("</flowList>");
		sb.append("<result>");
		if (resultObject!=null)
			sb.append("<className>").append(resultObject.getClass().getName()).append("</className>");
		if (resultMapping!=null)
			sb.append(resultMapping.toXml());
		sb.append("</result></processFlow>");
		
		try {
			return XmlUtils.format(sb.toString());
		} catch (Exception e) {
			LogUtils.exceptionLog(logger, e);
			return sb.toString();
		}
	}
}
