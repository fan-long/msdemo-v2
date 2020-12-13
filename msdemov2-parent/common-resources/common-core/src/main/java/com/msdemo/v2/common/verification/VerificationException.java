package com.msdemo.v2.common.verification;

import com.msdemo.v2.common.exception.TransException;

public class VerificationException extends TransException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2096569339741865194L;

	
	public static final String HANDLER_INSTANCE_ERROR="FFF0";
	
	public static final String TELLER_ERROR="FFF1";
	
	public VerificationException(String code, String message) {
		super(code, message);
	}

}
