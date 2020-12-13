package com.msdemo.v2.common.dtx.compensation;

import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.compensation.ICompensatable.Empty;
import com.msdemo.v2.common.dtx.core.AbsDtxContext;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.core.MainContext;
import com.msdemo.v2.common.dtx.core.StageContext;
import com.msdemo.v2.common.dtx.lock.ITxnLockAgent;
import com.msdemo.v2.common.dtx.lock.ITxnLockAgent.UnlockInfo;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.common.lock.model.ResourceLock.LockLevel;
import com.msdemo.v2.resource.redis.ClassSerializeUtil;


@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CdtxAspect {
	
	@Autowired
	ITxnLockAgent lock;	

	@Autowired
	IDtxCoordinator dtxManager;
	
	private static SpelExpressionParser parser = new SpelExpressionParser();

	private static final Logger logger =LoggerFactory.getLogger(CdtxAspect.class);
	
	@Around("@annotation(transaction)")
	public Object doTransaction(ProceedingJoinPoint pjd, CompensatableTransactional transaction) 
			throws Throwable{		
		//GlobalTxnId transmitted via Feign interceptor and Servlet filter
		if (TransContext.isDistributedTransaction()){
			//execute within a existed Global Transaction, join it
			return doStageTransaction(pjd,transaction);
		}else{
			if (transaction.entry()){
				//create a new Global Transaction
				return doMainTransaction(pjd,transaction);
			}else{
				StageContext localContext = new StageContext();
				localContext.setDtxId(TransContext.get().common.getTraceId());
				HashSet<UnlockInfo> unlockInfo=doLock(localContext,pjd.getArgs(),transaction.lock());
				//not in a global transaction context, execute without Global Transaction
				Object result= pjd.proceed();
				//TODO: unlock before local transaction committed
				if (unlockInfo!=null){
					for (UnlockInfo unlock: unlockInfo)
						lock.unlock(unlock);
				}
				return result;
			}
		}
							
	}
	
	private Object doMainTransaction(ProceedingJoinPoint pjd, CompensatableTransactional transaction) throws Throwable{
		MainContext context;
		if (MainContext.get()!=null){
			if(MainContext.get().isRunning()) 
				throw new RuntimeException("unsupported nested saga main transactional");
			else
				context=MainContext.get();
		}else
			context=dtxManager.createMainContext(null);
		context.setJoinPoint(pjd);
		context.setCdtxConfig(transaction);
		dtxManager.start(context);

		try {			
			Object result= pjd.proceed();
			context.setResponse(result);
			if (!context.isOutSourcing()){
				TransContext.verifyDeadline();
				dtxManager.commit(context.getDtxId(),false,result);
			}
			MainContext.get().getJournal().setTxnStatus(TxnStatus.COMPLETED.getValue());
			return result;
		}catch(Throwable e){
			//trigger roll back, compensation asynchronous
			dtxManager.rollback(context.getJournal().getTraceId(),context.getDtxId(),e.getMessage());
			MainContext.get().getJournal().setStatus(JournalStatus.TERMINATING.getValue());
			throw e;
		}

	}
	
	private Object doStageTransaction(ProceedingJoinPoint pjd, CompensatableTransactional transaction) throws Throwable{
		if (StringUtils.isBlank(transaction.compensator()) &&
				transaction.annotationType().equals(Empty.class))
			throw new IllegalArgumentException("compensator or compensatorClass must be provided");
		StageContext context;
		if (StageContext.get()!=null) {
			if (StageContext.get().isRunning())
				throw new RuntimeException("unsupported nested saga stage transactional");
			else
				context=StageContext.get();
		}else
			context=dtxManager.createStageContext(null);
		
		context.setJoinPoint(pjd);
		context.setCdtxConfig(transaction);
		dtxManager.stagePrepare(context);
		
		try {			
			HashSet<UnlockInfo> lockedKeys= doLock(context,pjd.getArgs(),transaction.lock());
			if (lockedKeys!=null && lockedKeys.size()>0){
				context.getJournal().setLocks(lockedKeys);
			}
			Object result= pjd.proceed();
			context.setResponse(result);
			dtxManager.stageCommit(context);
			return result;
		}catch(Throwable e){
			logger.error("dtx stage failed: {}",e.getMessage());
			context.setResponse(e.getMessage());
			dtxManager.stageRollback(context);
			throw e;
		}
	}
	private HashSet<UnlockInfo> doLock(AbsDtxContext<?> context,Object[] args,String[] expressions){
		if (!LockLevel.isLock(TransContext.get().common.getTxn().getLock()) || 
				expressions==null || expressions.length==0) return null;
		HashSet<String> locks= new HashSet<>(expressions.length,1);
		HashSet<UnlockInfo> unlockInfoSet=new HashSet<>(1);
		try {				
			for (String expression: expressions){
				//TODO: check arguments count
				String resourceKey=parser.parseExpression(expression).getValue(args).toString();
				//to prevent duplicate lock same resource within a process
				if (context.getLocks()==null || !context.getLocks().contains(resourceKey))
					locks.add(resourceKey);
			}
			for (String resourceKey:locks){							
				UnlockInfo unlockInfo=lock.lock(context.getDtxId(),resourceKey, 
						LockLevel.valueOf(TransContext.get().common.getTxn().getLock()),
						IDtxCoordinator.DEFAULT_TIME_OUT_MILLIS);
				unlockInfoSet.add(unlockInfo);
				TransContext.get().exchange.oneway.getUnLockList()
					.add(ClassSerializeUtil.serialize(unlockInfo));
			}
			return unlockInfoSet;
			
		} catch (Exception e) {
			try {
				for (UnlockInfo unlockInfo:unlockInfoSet){	
					lock.unlock(unlockInfo);
				}
			} catch (Exception unlockException) {
				//ignore;
			}
			throw e;
		}
	}	
		
}
