package com.msdemo.v2.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

	private static final ObjectMapper om= new ObjectMapper();

	static{
		om.setSerializationInclusion(Include.NON_NULL);  
	}
	
	public static <T> T readValue(String json, Class<T> clz){
		try {
			return (T)om.readValue(json, clz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String writeValue(Object obj){
		if (obj==null) return "null";
		try {
			return om.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return obj.toString();
		}
	}
}
