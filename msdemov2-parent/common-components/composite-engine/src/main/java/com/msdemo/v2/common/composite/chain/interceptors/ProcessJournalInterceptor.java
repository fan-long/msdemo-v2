package com.msdemo.v2.common.composite.chain.interceptors;

import java.time.Instant;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition.TxnType;
import com.msdemo.v2.common.composite.chain.AbsProcessInterceptor;
import com.msdemo.v2.common.composite.journal.ProcessJournal;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.core.MainContext;
import com.msdemo.v2.common.journal.broker.InvocationJournalBroker;
import com.msdemo.v2.common.journal.broker.JournalException;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.common.utils.LogUtil;
import com.msdemo.v2.resource.management.SpringContextHolder;
import com.msdemo.v2.resource.management.debug.DebugUtil;

@Component
public class ProcessJournalInterceptor extends AbsProcessInterceptor {

	private static final Logger logger =LoggerFactory.getLogger(ProcessJournalInterceptor.class);
	private static ThreadLocal<Long> startMillis= new ThreadLocal<>();
	
	@Autowired
	InvocationJournalBroker journalBroker;
	@Autowired
	IDtxCoordinator dtxManager;
	
	@Override
	protected void pre(InterceptorModel model){
		startMillis.set(Instant.now().toEpochMilli());
		//TODO: check flag if enabled to record journal
		
		ProcessJournal journal;
		if (TxnType.maybeDtxProcess(model.getDefinition().getTxnType())){
			//mark dtx to outsouring mode, as well as use external journal and commit management
			MainContext context= dtxManager.createMainContext(new ProcessJournal());
			journal = (ProcessJournal)context.getJournal();
			journal.setType(JournalType.DTX_MAIN.getValue());
			journal.setTxnId(context.getDtxId());
			journal.setTxnStatus(TxnStatus.RUNNING.getValue());
			journal.setDeadline(new Date(TransContext.get().common.getDeadline()));			
			journal.setAsync(journalBroker.getConfig().isAsync());				
		}else{
			journal = new ProcessJournal();
			journal.setType(JournalType.PROCESS.getValue());
		}
		journal.setTraceId(TransContext.get().common.getTraceId());
		journal.setSequenceId(0);
		journal.setAcctDate(TransContext.get().common.getAcctDate());
		journal.setStatus(JournalStatus.RUNNING.getValue());
		journal.setApplication(SpringContextHolder.getApplicationName());
		journal.setTransCode(model.getDefinition().getName());
					
		journal.setReqObject(model.getRequestDto());
		journalBroker.isolateInsert(journal);
	};
	
	@Override
    protected CompositionContext intercept(InterceptorModel model){
		try{
			return super.intercept(model);
		}catch(JournalException e){
			if (e.isDuplicate()) //caused by this.pre()
				throw e;
			else{
				updateStatus(TxnType.maybeDtxProcess(model.getDefinition().getTxnType())?JournalType.DTX_MAIN:JournalType.PROCESS,
					JournalStatus.FAILED,e.getMessage());
				throw e;
			}				
		}catch(Exception e){
			updateStatus(TxnType.maybeDtxProcess(model.getDefinition().getTxnType())?JournalType.DTX_MAIN:JournalType.PROCESS,
					JournalStatus.FAILED,e.getMessage());
			throw e;			
		}
	}
	@Override
	protected void post(InterceptorModel model){
		LogUtil.cost(logger, startMillis.get(), model.getDefinition().getName());
		if (TransContext.get().common.getTxn().getDtxId()!=null){
			DebugUtil.trigger();
			TransContext.verifyDeadline();
			updateStatus(JournalType.DTX_MAIN,
					JournalStatus.COMPLETED,model.getContext().getResp());
			dtxManager.commit(MainContext.get().getDtxId(),true,null);			
		}else
			updateStatus((MainContext.get()!=null && MainContext.get().getJournal()!=null)
					?JournalType.DTX_MAIN:JournalType.PROCESS,
				JournalStatus.COMPLETED,model.getContext().getResp());		
    	//mark composition context to null for GC
		Object resp= model.getContext().getResp();
    	model.getContext().clear();
    	model.getContext().setResp(resp);
	}
	
	private void updateStatus(JournalType type,JournalStatus status,Object resp){
		TxnStatus txnStatus =TxnStatus.COMPLETED;
		if (status.equals(JournalStatus.FAILED)){
			if (TransContext.isDistributedTransaction() && TransContext.get().common.getTxn().getDtxId()!=null){
				if (MainContext.get().getJournal().getStatus()==JournalStatus.TERMINATING.getValue())
					//already handler by dtx manager
					return;
				else if (MainContext.get().getJournal().getTxnStatus()==TxnStatus.COMPLETED.getValue()){
					logger.warn("dtx completed, but roll back required due to trans error: {}",resp);
					dtxManager.rollback(MainContext.get().getJournal().getTraceId(), 
							MainContext.get().getJournal().getTxnId(), (String)resp);
					return;
				}			
			}
			txnStatus=TxnStatus.FAILED;
		}
		ProcessJournal journal = new ProcessJournal();
		journal.setTraceId(TransContext.get().common.getTraceId());
		journal.setSequenceId(0);
		journal.setType(type.getValue());		
		journal.setStatus(status.getValue());		
		journal.setTxnStatus(txnStatus.getValue());
		journal.setElapsed(Instant.now().toEpochMilli() - startMillis.get());
		if (resp!=null)
			journal.setRespObject(resp);
		journalBroker.isolateUpdate(journal);
		
	}
	
	
}
