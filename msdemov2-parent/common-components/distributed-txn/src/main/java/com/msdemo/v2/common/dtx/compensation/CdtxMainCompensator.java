package com.msdemo.v2.common.dtx.compensation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.journal.DtxJournalQueryService;
import com.msdemo.v2.common.dtx.journal.DtxMainJournal;
import com.msdemo.v2.common.dtx.journal.DtxStageJournal;
import com.msdemo.v2.common.dtx.lock.ITxnLockAgent;
import com.msdemo.v2.common.dtx.lock.ITxnLockAgent.UnlockInfo;
import com.msdemo.v2.common.invocation.journal.ServiceCallerJournal;
import com.msdemo.v2.common.journal.broker.InvocationJournalBroker;
import com.msdemo.v2.common.journal.model.AbsInvocationJournal;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.common.sequence.increment.IIncrementIdGenerator;
import com.msdemo.v2.common.utils.LogUtil;

@Component
public class CdtxMainCompensator {

	private static final Logger logger =LoggerFactory.getLogger(CdtxMainCompensator.class);

	public static final int MAX_RETRY=3;

	@Autowired
	InvocationJournalBroker journalBroker;
	@Autowired
	DtxJournalQueryService dtxQueryService;
	@Autowired
	IStageCompensator stageCompensator;
	@Autowired
	ITxnLockAgent txnLock;
	@Autowired
	IIncrementIdGenerator dtxSequenceGenerator;
	
	
	@Async
	public void compensate(String dtxId){
		IDtxCoordinator.applyTrace(dtxId);
		DtxMainJournal mainJournal=dtxQueryService.queryTxnJournals(dtxId);
		logger.debug("{} compensatable records found",mainJournal.getSortedStageJournals().size());
		if (mainJournal.getStatus()==JournalStatus.NOTFOUND.getValue()){
			if (mainJournal.getCompensationJournal()==null){
				//should never occured unless DTX_COMPENSATION.async be set to true
				logger.warn("dtx# {} can not be compensated, wrong dtx# or "
						+ "need to wait for async DTX_COMPENSATION journal created"
						,dtxId);
				return;
			}else{
				logger.warn("dtx# {} can not be compensated, "
						+ "need to wait for async DTX_MAIN journal created"
						,dtxId);
				return;
			}
		}else if (mainJournal.getStatus()==JournalStatus.RUNNING.getValue()){
			if (mainJournal.getCompensationJournal()==null){
				//should never occurs unless DTX_COMPENSATION.async be set to true
				logger.warn("dtx# {} can not be compensated, need to wait for async DTX_COMPENSATION journal created"
						,dtxId);
				return;
			}
		}
		if (mainJournal.getStatus()!=JournalStatus.TERMINATING.getValue()){
			logger.warn("dtx# {} can not be compensated, main journal status should be TERMINATING but is {}."
					,dtxId, mainJournal!=null?JournalStatus.ofValue(mainJournal.getStatus()):JournalStatus.NOTFOUND);
			updateMainStatus(mainJournal,false);			
			return;
		}
		if (mainJournal.getCompensationJournal().getRetryCount()>=MAX_RETRY){
			logger.warn("dtx# {} compensation retry count: {} exceeds threshold {}",
					mainJournal.getCompensationJournal().getRetryCount(),MAX_RETRY);
			//MAX_RETRY should be managed by scheduler, ignore it to support manually compensation
		}
		if (mainJournal.isAsync()){
			long dtxStageCount=dtxSequenceGenerator.getIdByGroup(dtxId);
			if (dtxStageCount != mainJournal.getSortedStageJournals().size()){
				logger.info("dtx# {} compensation retry# {} failed, stage count {} but {} found",
					dtxId,	mainJournal.getCompensationJournal().getRetryCount(),
					dtxStageCount,mainJournal.getSortedStageJournals().size());
				updateMainStatus(mainJournal,false); //will try again later by compensation schedule task
				return;
			}
		}
		List<UnlockInfo> unlockInfoList= new ArrayList<>();
		try{
			for (int i=0;i<mainJournal.getSortedStageJournals().size();i++){
				AbsInvocationJournal<?> journal=mainJournal.getSortedStageJournals().get(i);
				if (journal instanceof DtxStageJournal){
					DtxStageJournal stageJournal= (DtxStageJournal) journal;
					if (stageJournal.getTxnStatus()==TxnStatus.COMPLETED.getValue()){
						stageCompensator.localStageCompensate(stageJournal);
						if (stageJournal.getLocks()!=null)
							unlockInfoList.addAll(stageJournal.getLocks());			
						logger.info("dtx# {} sequence# {}, stage {}-{} compensation completed",
								journal.getTxnId(),journal.getSequenceId(),stageJournal.getDomain(),stageJournal.getEndpoint());
					}else
						logger.debug("dtx# {} sequence# {} txnStatus: {}, stage {}-{} compensation ignored",
								journal.getTxnId(),journal.getSequenceId(),journal.getTxnStatus(),
									stageJournal.getDomain(),stageJournal.getEndpoint());
				}else if (journal instanceof ServiceCallerJournal){
					ServiceCallerJournal callerJournal= (ServiceCallerJournal) journal;
					if (callerJournal.isLocal()) continue; //skip caller journal without DTX
					if (callerJournal.getTxnStatus()==TxnStatus.RUNNING.getValue() ||
							callerJournal.getTxnStatus()==TxnStatus.COMPLETED.getValue()){
						DtxStageJournal remoteJournal=stageCompensator.callerCompensate(callerJournal);
						if (remoteJournal!=null){
							if (remoteJournal.getTxnStatus()==TxnStatus.COMPENSATED.getValue()){
								if (remoteJournal.getLocks()!=null)
									unlockInfoList.addAll(remoteJournal.getLocks());							
								logger.info("dtx# {} sequence# {}, caller {}-{} compensation completed",
										callerJournal.getTxnId(),callerJournal.getSequenceId()
										,callerJournal.getDomain(),callerJournal.getEndpoint());
							}else{
								logger.warn("dtx# {} sequence# {} status: {}, caller {}-{} compensation failed",
										journal.getTxnId(),journal.getSequenceId(),journal.getTxnStatus()
											,callerJournal.getDomain(),callerJournal.getEndpoint());
								throw new RuntimeException("remote caller of "+
										callerJournal.getDomain()+"-"
										+callerJournal.getEndpoint()+" compensation failed");
							}
						}
					}else
						logger.debug("dtx# {} sequence# {} status: {}, caller {}-{} compensation ignored",
								journal.getTxnId(),journal.getSequenceId(),journal.getTxnStatus()
								,callerJournal.getDomain(),callerJournal.getEndpoint());

				}
			}
		}catch (Exception e){
			LogUtil.exceptionLog(logger, e);
			mainJournal.getCompensationJournal().setErrorMsg(e.getMessage());
			updateMainStatus(mainJournal,false);
			return;
		}
		updateMainStatus(mainJournal,true);
		txnLock.unlock(unlockInfoList);
	}
	
	public void updateMainStatus(DtxMainJournal mainJournal, boolean isSuccess){
		if (isSuccess){
			journalBroker.updateStatus(mainJournal,JournalStatus.FAILED,TxnStatus.COMPENSATED);
			journalBroker.updateStatus(mainJournal.getCompensationJournal(),JournalStatus.COMPLETED,TxnStatus.COMPLETED);			
		}else{
			if (mainJournal.getCompensationJournal().getRetryCount()==MAX_RETRY-1){
				mainJournal.getCompensationJournal().setStatus(JournalStatus.FAILED.getValue());
				mainJournal.getCompensationJournal().setTxnStatus(TxnStatus.TERMINATED.getValue());
				mainJournal.getCompensationJournal().setRetryCount(MAX_RETRY);	
			}else{
				mainJournal.getCompensationJournal().setTxnStatus(TxnStatus.FAILED.getValue());
				mainJournal.getCompensationJournal().setDeadline(defaultExpirationTime());
				mainJournal.getCompensationJournal().setRetryCount(
					mainJournal.getCompensationJournal().getRetryCount()+1);
			}
			journalBroker.update(mainJournal.getCompensationJournal());
		}
	}
	
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void markRollback(String traceId,String dtxId,String exceptionMsg) {
			DtxMainJournal journal = new DtxMainJournal();
			journal.setTraceId(traceId);
			journal.setSequenceId(0);
			journal.setType(JournalType.DTX_COMPENSATION.getValue());
			journal.setTxnId(dtxId);
			journal.setStatus(JournalStatus.FAILED.getValue());
			journal.setRetryCount(0);
			journal.setDeadline(defaultExpirationTime());
			journal.setTxnStatus(TxnStatus.COMPENSATING.getValue());
			journal.setErrorMsg(exceptionMsg);
			//create compensation journal
			journalBroker.insert(journal);
			journal.setType(JournalType.DTX_MAIN.getValue());
			journal.setStatus(JournalStatus.TERMINATING.getValue());
			//update main journal status
			journalBroker.update(journal);		
	}
	
	private static Date defaultExpirationTime(){
		return new Date(Instant.now().plusSeconds(IDtxCoordinator.DEFAULT_TIME_OUT).toEpochMilli());
	}
}
