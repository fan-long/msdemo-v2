package com.msdemo.v2.common.exception;

public class ServiceNotFoundException extends TransException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3595604312869244860L;
	public static final String ERROR_CODE="0004";

	public ServiceNotFoundException(String serviceName) {
		super(ERROR_CODE,String.format("error occured on service: %s ", serviceName));
	}

	public ServiceNotFoundException(String serviceName,Exception e) {
		super(ERROR_CODE,String.format("error occured on service: %s ", serviceName),e);
	}
}
