package com.msdemo.v2.common.exception;

import com.msdemo.v2.common.exception.TransException;
public class ScriptNotFoundException extends TransException{

	private static final long serialVersionUID = -2146179811308705794L;
	public static final String ERROR_CODE="0005";

	public ScriptNotFoundException(String id) {
		super(ERROR_CODE,String.format("script not found: %s ", id));
	}
}
