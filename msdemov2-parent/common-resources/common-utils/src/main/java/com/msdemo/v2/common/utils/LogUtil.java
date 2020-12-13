package com.msdemo.v2.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);
	
	public static void cost(Logger log, long start,String msg){
		Logger costLogger=log==null?logger:log;
		if (costLogger.isDebugEnabled())
			costLogger.debug("{}_cost: {}ms",msg,Instant.now().toEpochMilli()-start);
	}
	public static void toJsonString(Logger log,Object o){
		Logger jsonLogger=log==null?logger:log;
		if (jsonLogger.isInfoEnabled())
			jsonLogger.info(JsonUtil.writeValue(o));
	}
	public static String getExceptionStack(Throwable t){
		try {
			StringWriter sw= new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			String result=sw.getBuffer().toString();			
			pw.close();
			sw.close();
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return "";
		}
	}
	
	public static void exceptionLog(Logger log, Throwable e){
		(log==null?logger:log).error(getExceptionStack(e));
	}

	
}
