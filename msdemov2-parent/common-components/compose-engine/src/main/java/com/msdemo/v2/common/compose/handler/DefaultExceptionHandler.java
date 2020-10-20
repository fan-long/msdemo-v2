package com.msdemo.v2.common.compose.handler;

import java.util.HashMap;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.ProcessFlowException;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.exception.TransException;
import com.msdemo.v2.common.util.LogUtils;
import com.msdemo.v2.common.verification.VerificationException;

public class DefaultExceptionHandler implements IExceptionHandler {
	private ParamMapping mapping=null;

	@Override
	public void handle(String flowName, ProcessFlowContext context, Exception e) {
		//TODO: check VerificationException
		context.setExceptionFlow(flowName);
		if (e instanceof VerificationException){
			context.setException((VerificationException)e);
		}else{
			LogUtils.exceptionLog(null, e);
			Throwable cause=ExceptionUtils.getRootCause(e);
			if (cause instanceof TransException){
				context.setException((TransException)cause);
			}else
				context.setException(new ProcessFlowException(context,
					cause,"error on execute flow: " + flowName));
		}
		if (mapping!=null){
			HashMap<String,String> result =new HashMap<>();
			SpelExpressionParser parser = ParamMapping.parser;
			for (MutablePair<String,String> pair:mapping){
				Object value=parser.parseExpression(pair.getRight()).getValue(context);
				parser.parseExpression(pair.getLeft()).setValue(result,value);
			}
			context.setResp(result);
		}
	}

	@Override
	public void setMapping(ParamMapping mapping){
		this.mapping=mapping;
	}
}