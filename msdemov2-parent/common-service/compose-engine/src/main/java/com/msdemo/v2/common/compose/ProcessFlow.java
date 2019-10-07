package com.msdemo.v2.common.compose;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.msdemo.v2.common.compose.flow.AbstractFlow;
import com.msdemo.v2.common.compose.handler.IExceptionHandler;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.util.LogUtils;

public class ProcessFlow {

	private static Logger logger =LoggerFactory.getLogger(ProcessFlow.class);
	
	String name;
	TxnType txnType=TxnType.Non;
	IExceptionHandler exeptionHandler;
	List<AbstractFlow> flowList = new ArrayList<>();
	ParamMapping resultMapping;
	Object result;
	
	public ProcessFlow(String name){
		this.name=name;
	}
	
	public void verify(){
		flowList.forEach( flow ->{
			flow.verify();
		});
	}
	public ProcessFlowContext execute(ProcessFlowContext context,Object req){
		AbstractFlow flow=null;
		try{
			for (int i=0;i<flowList.size();i++){
				flow=flowList.get(i);
				if (!context.containsKey(flow.getName())){
					flow.execute(context);
					//if transaction type changed, stop the process and return to ProcessFlowFactory
					if (context.isTxnTypeChanged()) 
						return context;
				}
			};
			//response
			if (resultMapping!=null){
				SpelExpressionParser parser = ParamMapping.parser;
				for (MutablePair<String,String> pair:resultMapping){
					Object value=parser.parseExpression(pair.getRight()).getValue(context);
					parser.parseExpression(pair.getLeft()).setValue(result,value);
				}
			}
			context.setResp(result);			
		}catch(Exception e){
			this.exeptionHandler.handle(flow, context, e);
		}
		return context;
	}
	public ProcessFlowContext execute(Object req){
		long start=System.currentTimeMillis();
		ProcessFlowContext context = new ProcessFlowContext(this.name,this.txnType);
		context.setReq(req);
		try {
			return execute(context,req);
		} finally{
			LogUtils.cost(logger, start, name);
		}
	}
	
	public enum TxnType{
		Global,Local,Dynamic,Non;
	}
}
