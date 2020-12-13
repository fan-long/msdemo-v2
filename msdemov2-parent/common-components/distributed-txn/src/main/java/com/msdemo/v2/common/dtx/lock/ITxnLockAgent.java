package com.msdemo.v2.common.dtx.lock;

import java.util.List;

import com.msdemo.v2.common.exception.TransException;
import com.msdemo.v2.common.lock.model.ResourceLock.LockLevel;

public interface ITxnLockAgent  {
	
	default void lock(String locker,String[] resourceIds,LockLevel level,int timeout){
		for (String key:resourceIds){
			lock(locker,key,level,timeout);
		}
	}
	
	//lock locally
	UnlockInfo lock(String locker,String resourceId,LockLevel level,int timeoutMillis) throws TxnLockException;
	void relock(UnlockInfo unlockInfo,int timeoutMillis) throws TxnLockException;
	//unlock locally or remotely
	void unlock(UnlockInfo unlockInfo);
	void unlock(List<UnlockInfo> unlockInfoList);
	void asyncUnlock(List<String> unlockInfoList);
	UnlockInfo buildUnlockInfo(String locker,String resourceId);
	
	public static class UnlockInfo{
		public String sourceApplication;
		public String targetApplication;
		public String locker;
		public String resourceId;
		public int retryCount;
	}
	
	public static class TxnLockException extends TransException{
		
		private static final long serialVersionUID = -3120434560340894147L;
		private static final String FAILED_MSM="resource: '%s', locker: '%s' failed";

		public static final String LOCK_FAILED="0100";
		public static final String UNLOCK_FAILED="0101";
		public static final String RELOCK_FAILED="0101";
		public TxnLockException(String code,String locker,String resourceId){
			super(code,String.format(FAILED_MSM,resourceId,locker));
		}
		public TxnLockException(String code,String locker,String resourceId, Exception e){
			super(code,String.format(FAILED_MSM,resourceId,locker),e);
		}
	}
}
