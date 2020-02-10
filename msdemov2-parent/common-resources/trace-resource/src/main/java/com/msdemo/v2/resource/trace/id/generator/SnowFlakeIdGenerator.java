package com.msdemo.v2.resource.trace.id.generator;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import com.msdemo.v2.resource.trace.id.ITransIdGenerator;


public class SnowFlakeIdGenerator implements ITransIdGenerator {
	private static final Logger logger =LoggerFactory.getLogger(SnowFlakeIdGenerator.class);

	@Value("${datacenter-id:1}")
	private short datacenterId;  //数据中心
	    
	@Value("${unit-id:1}")
	private short unitId;  //单元
	
	private long workerId; //from Redis, MAX value is 9999999;
	
	@Autowired
	RedisTemplate<String,Integer> redis;
	
	private static final String WORKER_ID_KEY="worker_id";
	private static final String ID_PATTERN="%s%s%s%s%s";
	private static final DateTimeFormatter FORMATTER= DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    /**
     * 每一部分占用的位数
     */
//    private final static long SEQUENCE_BIT = 12; //序列号占用的位数
     
    /**
     * 每一部分的最大值
     */
//    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);
        
    private int sequence = 0; //序列号
    private long lastStmp = -1L;//上一次时间戳
    
	@Override
	public String nextId() {
		long[] snowflake=snowflake();		
		return String.format(ID_PATTERN, ZonedDateTime.ofInstant ( 
					Instant.ofEpochMilli(snowflake[0]),ZoneOffset.ofHours(+8)).format(FORMATTER)
				,StringUtils.leftPad(String.valueOf(this.datacenterId), 2, '0')
				,StringUtils.leftPad(String.valueOf(this.unitId), 2,'0')
				,StringUtils.leftPad(String.valueOf(this.workerId), 7, '0')
				,StringUtils.leftPad(String.valueOf(snowflake[1]), 4, '0'));
	}

	private synchronized long[] snowflake(){
		long currStmp = Instant.now().toEpochMilli();//System.currentTimeMillis();
        if (currStmp < lastStmp) {
        	if (lastStmp - currStmp >= 10L){
        		this.workerId=loadWorkerId();
				logger.warn("clock moved backward, reload worker id: {}",this.workerId);
        	}else{
        		try {
					TimeUnit.MICROSECONDS.sleep(lastStmp - currStmp);
				} catch (InterruptedException e) {
					logger.warn("wait failed, reload worker id: {}",this.workerId);
	        		this.workerId=loadWorkerId();				
				}
        	}        		
        }
 
        if (currStmp == lastStmp) {
            //相同毫秒内，序列号自增
            sequence ++;
            //同一毫秒的序列数已经达到最大
            if (sequence % 4096 == 0) {
                currStmp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0;
        } 
        lastStmp = currStmp;
        return new long[]{lastStmp,sequence};
	}
	
	private long getNextMill() {
        long mill = Instant.now().toEpochMilli(); //System.currentTimeMillis();
        while (mill <= lastStmp) {
            mill = Instant.now().toEpochMilli(); //System.currentTimeMillis();
        }
        return mill;
    }

	@PostConstruct
	public void init(){
		this.workerId=loadWorkerId();
	}
	private long loadWorkerId(){
		long workerId=redis.opsForValue().increment(WORKER_ID_KEY);
		if (workerId<10000000)
			return workerId;
		else{
			//TODO: lock and reset redis workerid
			redis.opsForValue().set(WORKER_ID_KEY, 1);
			return 1;
		}
	}
}
