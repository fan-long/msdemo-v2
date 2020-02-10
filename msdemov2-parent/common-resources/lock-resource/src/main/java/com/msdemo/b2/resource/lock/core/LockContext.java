package com.msdemo.b2.resource.lock.core;

import java.time.Instant;
import java.util.Map;

public class LockContext {
	
	public static final String LOCKKEY="lock_key";
	public static final String LOCKER="locker";
	public static final String EXPIRETIME="expire_time";
	public static final String CREATETIME="create_time";
	public static final String CALLBACK="callback";
	public static final String COMMENTS="comments";
	
	private String lockKey;
	private String locker;
	private int timeout;
	private long expireTime;
	private long createTime;
	private String callback;
	private String comments;
	
	
	public LockContext(String key, String locker,int timeout, String callback,String comments){
		this.lockKey=key;
		this.locker=locker;
		this.timeout=timeout;
		this.createTime=Instant.now().toEpochMilli();
		this.expireTime=this.createTime + timeout;
		this.callback=callback;
		this.comments=comments;
	}
	
	public String getLockKey() {
		return lockKey;
	}
	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	public String getCallback() {
		return callback;
	}
	public void setCallback(String callback) {
		this.callback = callback;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	public String getLocker() {
		return locker;
	}
	public void setLocker(String locker) {
		this.locker = locker;
	}
	
	public static LockContext fromMap(Map<String,Object> lockMap){
		LockContext lock= new LockContext(lockMap.get(LOCKKEY).toString(),lockMap.get(LOCKER).toString(),
				0,lockMap.get(CALLBACK).toString(),lockMap.get(COMMENTS).toString());
		lock.setExpireTime(Long.parseLong(lockMap.get(EXPIRETIME).toString()));
		lock.setCreateTime(Long.parseLong(lockMap.get(CREATETIME).toString()));
		return lock;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}
}
