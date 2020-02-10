package com.msdemo.b2.resource.lock.core;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import com.msdemo.b2.resource.lock.config.DistributionLockConfiguration;
import com.msdemo.b2.resource.lock.spi.IDistributionLock;
import com.msdemo.v2.common.util.LogUtils;

/**
 * TODO: 使用evesha1优化LUA脚本调用
 * evalsha 命令根据给定的 sha1 校验码，执行缓存在服务器中的脚本。将脚本缓存到服务器的操作可以通过 SCRIPT LOAD 命令进行。
 * @author LONGFAN
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RedisLockService implements IDistributionLock {

	@Value("${"+DistributionLockConfiguration.LOCK_CONFIG_PREFIX+".redis.lock-prefix:lock}")
	String keyPrefix;
	
	@Value("${"+DistributionLockConfiguration.LOCK_CONFIG_PREFIX+".redis.auto-expire:false}")
	boolean autoExpire;
	
	@Value("${"+DistributionLockConfiguration.LOCK_CONFIG_PREFIX+".retry-period:5}")
	int retryPeriod;
	
	Logger logger = LoggerFactory.getLogger(getClass());

	private String KEY_PATTERN="";
	private String CONTEXT_PATTERN="";
	private String EXPIRE_PATTERN="";
	
	GenericJackson2JsonRedisSerializer redisSerializer = new GenericJackson2JsonRedisSerializer();		 

	@Autowired
	RedisTemplate<String,Object> redis;
	
	private static DefaultRedisScript redisLockScript = new DefaultRedisScript<String>();
	private static DefaultRedisScript redisUnlockScript = new DefaultRedisScript<String>();
	private static final String LockScript=
			"if redis.call('setNx',KEYS[1],ARGV[1]) then "+
		    "  if redis.call('get',KEYS[1])==ARGV[1] then "+
		    "    redis.call('set',KEYS[2],ARGV[3]) "+
		    "    redis.call('zadd',KEYS[3],ARGV[4],ARGV[5])"+
		    "    return 1" +		    
//	        "    return redis.call('expire',KEYS[1],ARGV[2]) "+
	        "  else "+
	        "    return 0 "+
	        "  end "+
	        "end";
	private static final String UnlockScript=
			"if redis.call('get',KEYS[1]) == ARGV[1] then"+
			"  redis.call('del',KEYS[2])"+
			"  redis.call('del',KEYS[3])"+		
			"  redis.call('del',KEYS[1])"+
			"  return 1 "+
			"else"+
			"  return 0 "+
			"end";
	
	@PostConstruct
	public void init(){
		System.out.println(LockScript);
		redisLockScript.setScriptText(LockScript);
		redisUnlockScript.setScriptText(UnlockScript);
		KEY_PATTERN=keyPrefix+":key:{%s}";
		CONTEXT_PATTERN=keyPrefix+":context:{%s}";
		EXPIRE_PATTERN=keyPrefix+":expire:{%s}";
	}
	/**
     * 原子方法尝试获取分布式锁  
     * @param lock对象 
     * @return 是否获取成功
     */
	@Override
	public boolean lock(LockContext lock) {

//		return ((Long) redis.execute(redisLockScript, Collections.singletonList(LOCK_PREFIX+lockKey), requestId,
//				expireTime)).equals(1L);
		
		return ((Long) redis.execute(
	            (SessionCallback) (RedisOperations ops) -> {
		            RedisConnection con=redis.getConnectionFactory().getConnection();
		            try {
						return con.eval(
								LockScript.getBytes(),
						        ReturnType.INTEGER,
						        3,
						        String.format(KEY_PATTERN,lock.getLockKey()).getBytes(),
						        String.format(CONTEXT_PATTERN,lock.getLockKey()).getBytes(),
						        String.format(EXPIRE_PATTERN,lock.getLockKey()).getBytes(),
						        lock.getLocker().getBytes(),
						        String.valueOf(lock.getTimeout()*1.0/1000).getBytes(),
						        redisSerializer.serialize(lock),
						        String.valueOf(lock.getExpireTime()).getBytes(),
						        lock.getLockKey().getBytes());
					} finally{
						RedisConnectionUtils.releaseConnection(con, redis.getConnectionFactory(),false);
					}
	            }
	    )).equals(1L);
	}
	
	@Override
	public  boolean unlock(String lockKey, String locker) {
//		return ((Long) redis.execute(redisUnlockScript, 
//			Collections.singletonList(AgentConstants.LOCK_PREFIX+lockKey), requestId)).equals(1L);
		return ((Long) redis.execute(
	            (SessionCallback) (RedisOperations ops) -> {
	            	RedisConnection con=redis.getConnectionFactory().getConnection();
		            try {
						return con.eval(
	            		UnlockScript.getBytes(),
	                    ReturnType.INTEGER,
	                    3,
	                    String.format(KEY_PATTERN,lockKey).getBytes(),
				        String.format(CONTEXT_PATTERN,lockKey).getBytes(),
				        String.format(EXPIRE_PATTERN,lockKey).getBytes(),
	                    locker.getBytes());
					} finally{
						RedisConnectionUtils.releaseConnection(con, redis.getConnectionFactory(),false);
					}
		           }
	    )).equals(1L);
		
	}

	@Override
	public boolean tryLock(LockContext lock, int waitMills) {
		int totalCount= waitMills/retryPeriod;
		int retryCount=0;
		long deadline=Instant.now().toEpochMilli() + waitMills;
		while(retryCount<=totalCount && Instant.now().toEpochMilli()<=deadline){
			if (lock(lock))
				return true;
			else{
				retryCount++;
				logger.debug("unable to lock [{}], retry #{}",lock.getLockKey(),retryCount);
				try {
					TimeUnit.MILLISECONDS.sleep(retryPeriod);
				} catch (InterruptedException e1) {
					LogUtils.exceptionLog(logger, e1);
				}
			}
		}			
		return false;
	}

	@Override
	public boolean relock(LockContext lock) {
		return lock(lock);
	}

	@Override
	public LockContext findLock(String key) {
		return (LockContext)redis.boundValueOps(String.format(CONTEXT_PATTERN,key)).get();
	}

	@Override
	public List<LockContext> topExpiredKeys(int count) {
		if (autoExpire)
			return null;
		else
			//TODO: get top count from sorted set
			return null;
	}
}
