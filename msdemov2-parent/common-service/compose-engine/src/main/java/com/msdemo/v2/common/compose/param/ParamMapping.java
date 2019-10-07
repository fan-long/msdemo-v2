package com.msdemo.v2.common.compose.param;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

public class ParamMapping extends ArrayList<MutablePair<String,String>> {

	public ParamMapping(){
		super();
	}
	public ParamMapping(int size){
		super(size);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 4183536892733103878L;

	public static SpelExpressionParser parser = new SpelExpressionParser();

	//TODO: check cache performance
	public static EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

	public ParamMapping add(String left,String right){
		this.add(new MutablePair<String, String>(left,right));
		return this;
	}

}
