package com.msdemo.v2.common.exception;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TransException extends RuntimeException {

	public static final String RESPONSE_CODE_SUCCESS="0000";
	
	public static final String RESPONSE_STATUS_NOT2XX="DDDD";
	public static final String RESPONSE_CODE_TIMEOUT="EEEE";
	public static final String RESPONSE_CODE_UNKNOWN_FAILED="FFFF"; 
	
	public static final String TRANS_LOG_UPDATE="FFFA";
	/**
	 * 
	 */
	private static final long serialVersionUID = -6457696064496371623L;

	private String errorCode;
	private String errorMsg;
	private List<String> appNameList = new ArrayList<String>();
	
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public TransException(String code, String message){
		super(message);
		this.errorCode=code;
		this.errorMsg=message;
	}
	
	public TransException(String code, String message, Exception e){
		super(message,e);
		this.errorCode=code;
		this.errorMsg=message;		
	}
		
	public void addAppName(Collection<String> appName){
		if (appName!=null) this.appNameList.addAll(appName);
	}
	public void addAppName(String appName){
		if (appName!=null) this.appNameList.add(appName);
	}
	
	public List<String> getAppNameList(){
		return this.appNameList;
	}
	public static TransException fromExceptionMsg(String exception){
		String[] tmp=StringUtils.substringsBetween(exception, "[", "]");
		TransException e;
		if (tmp.length ==2)
			e= new TransException(tmp[0],tmp[1]);
		else
			e= new TransException(RESPONSE_CODE_UNKNOWN_FAILED,exception);
		return e;
	}
	public static String toExceptionMsg(String errorCode,String errorMsg){
		return String.format("TransException: code [%s], message [%s]", 
				errorCode,errorMsg);
	}
}
