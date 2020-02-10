package com.msdemo.v2.common.check.duplicate;

import com.msdemo.v2.common.exception.TransException;

public class DupCheckException extends TransException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6572458640549366178L;

	public static final String ERROR_CODE="0001";
	
	public DupCheckException(String msg){
		super(ERROR_CODE,String.format("duplicate transaction, seq_no: %s", msg));
	}

}
