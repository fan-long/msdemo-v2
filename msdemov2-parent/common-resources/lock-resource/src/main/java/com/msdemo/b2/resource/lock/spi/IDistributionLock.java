package com.msdemo.b2.resource.lock.spi;

import java.util.List;

import com.msdemo.b2.resource.lock.core.LockContext;

public interface IDistributionLock {
	/**
	 * set lock without wait
	 * @param lock context
	 * @return true: Succeed, TODO: false: Reentrant
	 */
	boolean lock(LockContext lock) throws LockException;
	
	/**
	 * wait and set lock 
	 * @param lock context
	 * @return
	 */
	boolean tryLock(LockContext lock, int waitMills) throws LockException;
	
	/**
	 * reset lock expired time
	 * @param lock context
	 * @return
	 */
	boolean relock(LockContext lock) throws LockException;
	
	/**
	 * unlock
	 * @param key
	 * @return
	 */
	boolean unlock(String key,String locker) ;
	
	/**
	 * find and retrieve lock information
	 * @param key
	 * @return
	 */
	LockContext findLock(String key);
	
	/**
	 * retrieve top count of expired locks
	 * @param count of keys to retrieve
	 * @return
	 */
	List<LockContext> topExpiredKeys(int count);

}
