package com.msdemo.v2.common.composite.param;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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

	//config to auto expend collection -> new SpelParserConfiguration(false,true)
	public static SpelExpressionParser parser = new SpelExpressionParser();

	private Type type;

	//TODO: check cache performance
//	public static EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

	public ParamMapping targetType(Type type){
		this.type=type;
		return this;
	}
	public ParamMapping add(String left,String right){
		this.add(new MutablePair<String, String>(left,right));
		return this;
	}

	public StringBuilder toXml() {
		StringBuilder sb = new StringBuilder();
		if (type!=null)
			sb.append("<mappings type=\"").append(type.name()).append("\">");
		else
			sb.append("<mappings>");
		for (MutablePair<String, String> pair : this) {
			sb.append("<mapping target=\"").append(pair.left).append("\" source=\"").append(pair.right).append("\"/>");
		}
		sb.append("</mappings>");
		return sb;
	}
	public Type getType() {
		return this.type;
	}

	public static enum Type{
		List,Map;
	}
}
