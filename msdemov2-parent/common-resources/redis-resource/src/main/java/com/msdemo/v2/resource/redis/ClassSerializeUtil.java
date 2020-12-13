package com.msdemo.v2.resource.redis;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

public class ClassSerializeUtil {

	private static GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();		 

	public static String serialize(Object req){
		return new String(jackson2JsonRedisSerializer.serialize(req));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String json){
		return (T) jackson2JsonRedisSerializer.deserialize(json.getBytes());
	}
}
