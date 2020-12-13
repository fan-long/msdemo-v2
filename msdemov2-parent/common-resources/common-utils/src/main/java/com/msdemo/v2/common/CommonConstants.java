package com.msdemo.v2.common;

public class CommonConstants {
	
	public final static String BASE_PACKAGE="com.msdemo.v2";
//			String.join(".", Arrays.copyOfRange(CommonConstants.class.getPackage().getName()
//					.split("\\."), 0, 3));
	//配置中心框架相关配置项共同前缀
	public final static String CONFIG_ROOT_PREFIX="msdemo-config";
	
	public final static String CACHED_REQUEST_BODY_KEY="CACHED_REQUEST";
	public final static String CACHED_REPONSE_BODY_KEY="CACHED_RESPONSE";
	
	public final static String HEADER_TRANS_UNIQUE_ID_KEY="X-Trans-Unique-Id";
	public final static String HEADER_TRANS_CODE_KEY="X-Trans-Code";
	public final static String HEADER_TRANS_SEQNO_KEY="X-Trans-Seq-No";
	public final static String HEADER_SERIVCE_ROUTE_KEY="X-Micro-Service";
	public static final String HEADER_EXCEPTION_SERVICE="X-Exception-Service";
	
	public static final String HEADER_TRANS_COMMON_CONTEXT_KEY = "X-Trans-CC";
	public static final String HEADER_TRANS_EXCHANGE_CONTEXT_KEY = "X-Trans-EC";

	public final static String ACCOUNT_SERVICE_NAME="account-service";
	public final static String CUSTOMER_SERVICE_NAME="customer-service";
	public final static String OUTBOUND_SERVICE_NAME="outbound-service";
	public final static String SCHEDULER_SERVICE_NAME="scheduler-service";
	public final static String PARAMETER_SERVICE_NAME="parameter-service";
	
	public final static String ATOMIC_ENDPOINT_PATH="/atomic/";	
	public static final String COMMON_ASYNC_PATH="/async";
	
	public static final String DTX_ENDPOINT_PATH="/dtx/";
	public static final String DTX_STAGE_COMPENSATION_ENDPOINT=DTX_ENDPOINT_PATH+"stagecompensate";
	public static final String DTX_UNLOCK_ENDPOINT=DTX_ENDPOINT_PATH+"unlock";
	public static final String DTX_STATUS_ENDPOINT=DTX_ENDPOINT_PATH+"status";
	
	
	public static final String GENERIC_SERVICE_ENDPOINT="/generic";
	public static final String COMPOSITE_PROCESSES_PATH="/trans";
	public static final String TRANS_PORTAL_PATH=COMPOSITE_PROCESSES_PATH+"/portal";
	public static final String BATCH_JOB_ENDPOINT_PATH="/batch";

	public static final String KEY_SPLITTER=":";
	public static final String WORD_SPLITTER="-";
	public static final String LIST_SPLITTER=",";
	public static final String DEFAULT_CSV_DELIMITER="||";
	
	public static final String REMOTE_TIMER_KEY="remote-timer";
	
	public static final String MDC_TRACE_ID="traceId";
	public static final String MDC_ZONE_ID="zoneId";

	//bound unit id of current trans, set by remote caller (gateway, service invoker etc,.)
	public static final String LOCAL_BOUND_ZONE_ID="BOUND_ZONE_ID";
	//target unit id of remote invocation
	public static final String LOCAL_ROUTING_TARGET_KEY="ROUTING_TARGET";
	
	public static final String PARAM_DATASOURCE_PREFIX="param-";
	public static final String ROUTINE_DATASOURCE_PREFIX="routine-";
}
