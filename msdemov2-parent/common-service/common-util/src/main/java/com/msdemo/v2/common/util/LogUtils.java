package com.msdemo.v2.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(LogUtils.class);
	
	private static ObjectMapper om= new ObjectMapper();

	public static String toJsonString(Object o){
		if (o==null) return "null";
		try {
			return om.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			return o.toString();
		}
	}
	public static void cost(Logger log, long start,String msg){
		(log==null?logger:log).debug("{}_cost: {}ms",msg,System.currentTimeMillis()-start);
	}
	public static void toJsonString(Logger log,Object o){
		(log==null?logger:log).info(toJsonString(o));
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
