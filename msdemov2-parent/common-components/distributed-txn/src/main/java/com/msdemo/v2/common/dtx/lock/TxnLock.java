package com.msdemo.v2.common.dtx.lock;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.journal.DtxJournalQueryService;
import com.msdemo.v2.common.generic.executor.IRemoteGenericExecutor;
import com.msdemo.v2.common.invocation.endpoint.RemoteEndpointInvoker;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.common.lock.core.DelegateLockService;
import com.msdemo.v2.common.lock.core.ResourceType;
import com.msdemo.v2.common.lock.model.ResourceLock;
import com.msdemo.v2.common.lock.model.ResourceLock.LockLevel;
import com.msdemo.v2.common.lock.spi.IExpiredLockCallback;
import com.msdemo.v2.resource.management.SpringContextHolder;
import com.msdemo.v2.resource.redis.ClassSerializeUtil;

public class TxnLock implements ITxnLockAgent,IExpiredLockCallback{
	
	public static final int MAX_UNLOCK_RETRY_COUNT=3;
	
	private static Logger logger =LoggerFactory.getLogger(TxnLock.class);
	
	@Autowired
	protected DelegateLockService lockBroker;
	
	@Autowired
	RemoteEndpointInvoker endpointInvoker;
	
	@Autowired
	DtxJournalQueryService dtxQueryService;
	
	@Override
	public UnlockInfo lock(String locker,String resourceId,LockLevel level,int timeoutMillis) {
		UnlockInfo unlockInfo =buildUnlockInfo(locker,resourceId);
		ResourceLock lock = 
				ResourceLock.builder().resource(resourceId)
				.locker(locker)
				.level(level)
				.callback(this)
				.comments(ClassSerializeUtil.serialize(unlockInfo))
				.timeoutMillis(timeoutMillis>0?timeoutMillis:(IDtxCoordinator.DEFAULT_TIME_OUT_MILLIS)).build();
		if(lockBroker.get(ResourceType.Txn).lock(lock)){
			logger.info("[{}] locked, level: {}",resourceId,level.name());
			return unlockInfo;
		}else
			throw new TxnLockException(TxnLockException.LOCK_FAILED,locker,resourceId);
	}	
	
	@Override
	public void unlock(UnlockInfo unlockInfo){
//		if (!TransContext.get().common.isAllowTxnLock()) return;
		boolean result;
		if (StringUtils.equals(unlockInfo.targetApplication,SpringContextHolder.getApplicationName())){
			result =lockBroker.get(ResourceType.Txn).unlock(unlockInfo.resourceId,unlockInfo.locker);
		}else{
			result=endpointInvoker.invoke(unlockInfo.targetApplication,CommonConstants.DTX_UNLOCK_ENDPOINT,
							unlockInfo, Boolean.class);
		}
		if (result)
			logger.debug("[{}] unlocked",unlockInfo.resourceId);
		else
			throw new TxnLockException(TxnLockException.UNLOCK_FAILED,unlockInfo.locker,unlockInfo.resourceId);
	
	}
	
	@Override
	public UnlockInfo buildUnlockInfo(String locker, String resourceId) {
		UnlockInfo unlockInfo = new UnlockInfo();
		unlockInfo.locker=locker;
		unlockInfo.resourceId=resourceId;
		unlockInfo.sourceApplication=(String) TransContext.get().local.get(IRemoteGenericExecutor.REQUEST_INFO_SOURCE_APPLICATION);
		unlockInfo.targetApplication=SpringContextHolder.getApplicationName();
		return unlockInfo;
	}

	@Override
	public void relock(UnlockInfo info, int timeoutMillis) throws TxnLockException {
		ResourceLock lock = 
				ResourceLock.builder().resource(info.resourceId)
				.locker(info.locker)
				.callback(this)
				.comments(ClassSerializeUtil.serialize(info))
				.timeoutMillis(timeoutMillis>0?timeoutMillis:(IDtxCoordinator.DEFAULT_TIME_OUT_MILLIS)).build();		
		lockBroker.get(ResourceType.Txn).relock(lock,timeoutMillis>0?timeoutMillis:(IDtxCoordinator.DEFAULT_TIME_OUT_MILLIS));
		logger.info("[{}] relocked",info.resourceId);
	}

	@Override
	public boolean onLockTimedout(String value) {
		if (StringUtils.isBlank(value)) return true;
		UnlockInfo info = ClassSerializeUtil.deserialize(value);
		IDtxCoordinator.applyTrace(info.locker);
		logger.info("lock key [{}] timed out",info.resourceId);
		TxnStatus status=dtxQueryService.queryStatus(info.locker);
		if (TxnStatus.isFinalStatus(status)){ 
			unlock(info);
			return true;
		}else{
			info.retryCount++;
			if (info.retryCount<MAX_UNLOCK_RETRY_COUNT){
				relock(info,0);				
				return true;
			}else{
				logger.warn("exceeds max retry of {}, failed to unlock timed out key [{}], dtx# [{}] status is {}", 
						MAX_UNLOCK_RETRY_COUNT, info.resourceId,info.locker,status);
				return false; //to trigger fail over
			}
		}
		
	}

	@Override
	@Async
	public void asyncUnlock(List<String> unlockInfoList) {
		for (String info: unlockInfoList){
			this.unlock((UnlockInfo)ClassSerializeUtil.deserialize(info));
		}		
	}

	public void unlock(List<UnlockInfo> unlockInfoList) {
		for (UnlockInfo info: unlockInfoList){
			this.unlock(info);
		}		
	}
}
