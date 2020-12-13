package com.msdemo.v2.common.composite.handler;

import java.util.HashMap;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.CompositionException;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.exception.TransException;
import com.msdemo.v2.common.verification.VerificationException;

public class DefaultExceptionHandler implements IExceptionHandler {
	private ParamMapping mapping=null;

	@Override
	public void handle(String flowName, CompositionContext context, Exception e) {
		//TODO: check VerificationException
		context.setExceptionFlow(flowName);
		if (e instanceof VerificationException){
			context.setException((VerificationException)e);
		}else{
//			LogUtils.exceptionLog(null, e);
			Throwable cause=ExceptionUtils.getRootCause(e);
			if (cause instanceof TransException){
				context.setException((TransException)cause);
			}else
				context.setException(new CompositionException(context,
					cause,"error on execute flow: " + flowName +", msg: "+cause.getMessage()));
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
