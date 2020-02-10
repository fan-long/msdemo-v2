package com.msdemo.v2.resource.trace.id.generator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import com.msdemo.v2.resource.trace.id.ITransIdGenerator;

public class RedisIdGenerator implements ITransIdGenerator {

	@Autowired
	RedisTemplate<String,Object> redis;
	
	@Override
	public String nextId() {
		RedisAtomicLong redisId= new RedisAtomicLong(TRACE_ID,
				redis.getConnectionFactory());
		return String.valueOf(redisId.incrementAndGet());
	}

}


