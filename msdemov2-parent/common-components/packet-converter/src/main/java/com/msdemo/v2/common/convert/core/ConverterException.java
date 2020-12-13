package com.msdemo.v2.common.convert.core;

public class ConverterException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4312323454328167509L;

	public static final String REQUIRED_FIELD="%s:%s field is requreid";
	public static final String DTO_INSTANCE="create new instance of %s failed: %s";
	public static final String METHOD_MISSING="'%s' method not implemented";
	public static final String PARSER_FAILED="'%s' failed due to %s";

	public ConverterException(String msg){
		super(msg);
	}
}
