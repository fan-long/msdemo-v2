package com.msdemo.v2.common.dtx.compensation;

import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.core.MainContext;
import com.msdemo.v2.common.dtx.core.StageContext;
import com.msdemo.v2.common.dtx.journal.DtxJournalQueryService;
import com.msdemo.v2.common.dtx.journal.DtxMainJournal;
import com.msdemo.v2.common.dtx.journal.DtxStageJournal;
import com.msdemo.v2.common.dtx.lock.ITxnLockAgent;
import com.msdemo.v2.common.journal.broker.InvocationJournalBroker;
import com.msdemo.v2.common.journal.broker.JournalException;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.common.sequence.increment.IIncrementIdGenerator;
import com.msdemo.v2.common.timing.domain.TimingEventListener;
import com.msdemo.v2.resource.management.SpringContextHolder;
import com.msdemo.v2.resource.redis.ClassSerializeUtil;


public class CdtxCoordinator implements IDtxCoordinator {

	private static final Logger logger =LoggerFactory.getLogger(CdtxCoordinator.class);
	@Autowired
	InvocationJournalBroker journalBroker;
	@Autowired
	DtxJournalQueryService dtxQueryService;
	@Autowired
	CdtxMainCompensator compensator;
	
	@Autowired
	ITxnLockAgent txnLock;
	
	@Autowired
	IIncrementIdGenerator stageIdGenerator;
	
	TimingEventListener expirationListener;
	
	public CdtxCoordinator(TimingEventListener expirationListener){
		this.expirationListener=expirationListener;
	}
	
	@Override
	public MainContext createMainContext(DtxMainJournal journal) {
		MainContext context = journal==null?new MainContext():new MainContext(journal);
		context.setDtxId(this.buildDtxId());
		MainContext.set(context);
		return context;
	}

	@Override
	public StageContext createStageContext(DtxStageJournal journal) {
		StageContext context = journal==null?new StageContext():new StageContext(journal);
		context.setDtxId(this.buildDtxId());
		StageContext.set(context);
		return context;
	}
	
	@Override
	public void start(MainContext context){
		//start dtx
		TransContext.get().common.getTxn().setDtxId(context.getDtxId());

		//apply lower deadline of external timeout and transaction timeout setting
		long dtxDeadline=Instant.now().plusSeconds(context.getCdtxConfig().timeout()).toEpochMilli();
		if (dtxDeadline<TransContext.get().common.getDeadline())
			TransContext.get().common.setDeadline(dtxDeadline);
		
		logger.debug("start dtx on compensation mode");
		if (!context.isOutSourcing()){
			//create main journal
			DtxMainJournal journal= new DtxMainJournal();
			context.setJournal(journal);
			journal.setTraceId(TransContext.get().common.getTraceId());
			journal.setSequenceId(0);
			journal.setType(JournalType.DTX_MAIN.getValue());
			journal.setAsync(journalBroker.getConfig().isAsync());				
			journal.setStatus(JournalStatus.RUNNING.getValue());
			journal.setTxnStatus(TxnStatus.RUNNING.getValue());
			journal.setTxnId(context.getDtxId());
			journal.setAcctDate(TransContext.get().common.getAcctDate());
			journal.setDeadline(new Date(TransContext.get().common.getDeadline()));
			journal.setRequest(ClassSerializeUtil.serialize(context.getJoinPoint().getArgs()));
			journalBroker.isolateInsert(journal);
			TransContext.get().nextSequence();
		}
		expirationListener.asyncAdd(context.getDtxId(), context.getCdtxConfig().timeout() * 1000);
	}
	
	@Override
	public void stagePrepare(StageContext context) {
		logger.debug("dtx stage start");
		context.setLocks(TransContext.get().exchange.twoway.getLocks());
		if (!context.isOutSourcing()){
			//create stage journal
			DtxStageJournal journal= new DtxStageJournal();
			context.setJournal(journal);
			journal.setTraceId(TransContext.get().common.getTraceId());
			journal.setSequenceId(TransContext.get().common.getSequenceId());
			journal.setType(JournalType.DTX_STAGE.getValue());
			journal.setDomain(SpringContextHolder.getApplicationName());
			String compensator=context.getCdtxConfig().compensator();
			journal.setEndpoint(StringUtils.isNotBlank(compensator)?compensator:
				context.getCdtxConfig().compensatorClass().getName());;
			journal.setStatus(JournalStatus.RUNNING.getValue());
			journal.setTxnStatus(TxnStatus.RUNNING.getValue());
			journal.setGeneric(false);
			journal.setElapsed(Instant.now().toEpochMilli());
			journal.setContext(TransContext.get());
			journal.setTxnId(context.getDtxId());
			journal.setRequest(ClassSerializeUtil.serialize(context.getJoinPoint().getArgs()));
			context.setJournal(journal);
		}
	}

	@Override
	public void stageCommit(StageContext context) {
		if (!context.isOutSourcing()){
			DtxStageJournal journal= context.getJournal();
			journal.setStatus(JournalStatus.COMPLETED.getValue());
			journal.setTxnStatus(TxnStatus.COMPLETED.getValue());
			journal.setRespObject(context.getResponse());
			journal.setElapsed(Instant.now().toEpochMilli() - journal.getElapsed());
			journalBroker.insert(journal);
			TransContext.get().nextSequence();
			if (journalBroker.getConfig().isAsync()){
				long stageIndex=stageIdGenerator.nextIdByGroup(TransContext.get().common.getTxn().getDtxId());
				logger.debug("dtx# {}: async journal stage {} started",TransContext.get().common.getTxn().getDtxId(),stageIndex);
			}
		}
		if (context.getJournal().getLocks()!=null){
			context.getJournal().getLocks().forEach( u -> {
				TransContext.get().exchange.twoway.getLocks().add(u.resourceId);
			});
		}
		TransContext.verifyDeadline();		
		
		if (context.isOutSourcing()){
			logger.debug("dtx stage {}-{} commit",
					context.getJournal().getDomain(),context.getJournal().getEndpoint());				
		}else
			logger.debug("dtx stage {}-{} commit",
				context.getJoinPoint().getTarget(),context.getJoinPoint().getSignature().getName());
		context.clean();
	}

	@Override
	public void stageRollback(StageContext context) {		
		if (context.isOutSourcing()){
			logger.debug("dtx stage {}-{} rollback",
				context.getJournal().getDomain(),context.getJournal().getEndpoint());				
		}else
			logger.debug("dtx stage {}-{} rollback",
				context.getJoinPoint().getTarget(),context.getJoinPoint().getSignature().getName());

		context.clean();
	}

	@Override
	public void rollback(String traceId,String dtxId,String exceptionMsg) {
		logger.debug("dtx rollback on compensation mode");		
		try {
			compensator.markRollback(traceId, dtxId, exceptionMsg);
		} catch (JournalException e) {
			if (e.isDuplicate()){
				//already rolled back, remove timeout listener only
				expirationListener.asyncRemove(dtxId);
				return;
			}
			throw e;
		}
		//trigger compensation asynchronous, sleuth to track async-thread
		compensator.compensate(dtxId);
	}

	
	@Override
	public void commit(String dtxId, boolean isManaged,Object response) {
		if (!isManaged){
			DtxMainJournal journal = new DtxMainJournal();
			journal.setTraceId(TransContext.get().common.getTraceId());
			journal.setSequenceId(0);
			journal.setType(JournalType.DTX_MAIN.getValue());
			journal.setStatus(JournalStatus.COMPLETED.getValue());
			journal.setTxnStatus(TxnStatus.COMPLETED.getValue());
			journal.setRespObject(response);
			try {
				journalBroker.isolateUpdate(journal);
			} catch (Exception e) {
				rollback(TransContext.get().common.getTraceId(),dtxId,e.getMessage());
				throw e;
			}
		}
		if (journalBroker.getConfig().isAsync())
			stageIdGenerator.removeIdByGroup(dtxId);
		expirationListener.asyncRemove(dtxId);
		txnLock.asyncUnlock(TransContext.get().exchange.oneway.getUnLockList());
		logger.info("dtx# {} commit",dtxId);
	}

	public TimingEventListener getExpirationListener(){
		return expirationListener;
	}
	@Override
	public boolean onTiming(String dtxId) {
		IDtxCoordinator.applyTrace(dtxId);
		DtxMainJournal journal=dtxQueryService.queryMainJournal(dtxId);
		TxnStatus status=dtxQueryService.convertTxnStatus(journal);
		if (TxnStatus.isFinalStatus(status) || status.equals(TxnStatus.COMPENSATING)){
			logger.info("dtx# {} timed out, ignored since status was {}",dtxId, status);
		}else{
			logger.info("dtx# {} timed out, status is {}, start to roll back",dtxId, status);
			rollback(journal.getTraceId(),dtxId,"timedout");
		}
		return true;
	}

	@Override
	public void failover(String dtxId) {
		logger.error("dtx# {} timed out failover, manual intervention required!",dtxId);		
	}

}
