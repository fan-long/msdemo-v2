package com.msdemo.v2.common.param.consistent;

import com.msdemo.v2.common.exception.TransException;

public class InconsistentVersionException extends TransException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3595604312869244860L;
	public static final String ERROR_CODE="0005";

	public InconsistentVersionException(String param, String correct, String incorrent) {
		super(ERROR_CODE,String.format("inconsistent param version on %s, need %s, get %s", param,correct,incorrent));
	}

	public InconsistentVersionException(String param, String correct, String incorrent,Exception e) {
		super(ERROR_CODE,String.format("inconsistent param version on %s, need %s, get %s", param,correct,incorrent),e);
	}
}
