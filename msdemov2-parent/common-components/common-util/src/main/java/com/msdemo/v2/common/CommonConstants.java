package com.msdemo.v2.common;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommonConstants {
	
	//配置中心框架相关配置项共同前缀
	public final static String CONFIG_ROOT_PREFIX="msdemo-config";
	
	public final static String CACHED_REQUEST_BODY_KEY="CACHED_REQUEST";
	public final static String CACHED_REPONSE_BODY_KEY="CACHED_RESPONSE";
	
	public final static String HEADER_TRANS_UNIQUE_ID_KEY="X-Trans-Unique-Id";
	public final static String HEADER_TRANS_CODE_KEY="X-Trans-Code";
	public final static String HEADER_TRANS_SEQNO_KEY="X-Trans-Seq-No";
	public final static String HEADER_SERIVCE_ROUTE_KEY="X-Micro-Service";
	public static final String HEADER_EXCEPTION_SERVICE="X-Exception-Service";
	
	public final static String ACCOUNT_SERVICE_NAME="account-service";
	public final static String CUSTOMER_SERVICE_NAME="customer-service";
	public final static String OUTBOUND_SERVICE_NAME="customer-service";
	
	public static final String COMMON_CONTROLLER_PATH="/trans-portal";
	
	public static final AtomicBoolean JVM_RUNNING_FLAG= new AtomicBoolean(true);

	public static final String KEY_SPLITTER=":";
	public static final String WORD_SPLITTER="-";
	public static final String LIST_SPLITTER=",";
}
