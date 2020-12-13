package com.msdemo.v2.common.dtx.compensation;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.dtx.journal.DtxJournalQueryService;
import com.msdemo.v2.common.dtx.journal.DtxStageJournal;
import com.msdemo.v2.common.generic.GenericRequest;
import com.msdemo.v2.common.generic.executor.LocalGenericExecutor;
import com.msdemo.v2.common.invocation.endpoint.RemoteEndpointInvoker;
import com.msdemo.v2.common.invocation.invoker.InvocationProxy;
import com.msdemo.v2.common.invocation.journal.ServiceCallerJournal;
import com.msdemo.v2.common.journal.broker.InvocationJournalBroker;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.resource.management.SpringContextHolder;
import com.msdemo.v2.resource.redis.ClassSerializeUtil;


public class CdtxStageCompensator implements IStageCompensator{

	private static final Logger logger =LoggerFactory.getLogger(CdtxStageCompensator.class);
	@Autowired
	InvocationJournalBroker journalBroker;
	@Autowired
	DtxJournalQueryService dtxQueryService;
	@Autowired
	LocalGenericExecutor localGenericExecutor;
	
	@Autowired
	InvocationProxy invocationProxy;

	@Autowired
	RemoteEndpointInvoker endpointInvoker;
	
	@Transactional
	public DtxStageJournal remoteStageCompensate(ServiceCallerJournal callerJournal){
		DtxStageJournal journal = dtxQueryService.queryStageJournal(
				callerJournal.getTxnId(), callerJournal.getSequenceId());
		DtxStageJournal result= new DtxStageJournal();			
		result.setTraceId(callerJournal.getTraceId());
		result.setSequenceId(callerJournal.getSequenceId());
		result.setTxnId(callerJournal.getTxnId());
		if (journal!=null){
			if (journal.getTxnStatus()==TxnStatus.RUNNING.getValue()){
				//should never happen since only preInsert could set status as RUNNING
				throw new RuntimeException("error TxnStatus: RUNNING of dtx# " + journal.getTxnId());
			}else if(journal.getTxnStatus()==TxnStatus.COMPLETED.getValue()){
				localStageCompensate(journal);
				result.setStatus(JournalStatus.COMPLETED.getValue());
				result.setTxnStatus(TxnStatus.COMPENSATED.getValue());				
				result.setLocks(journal.getLocks());
			}else{
				result.setTxnStatus(journal.getTxnStatus());
			}
		}else{
			//insert journal to prevent duplicate compensation 
			//and terminate running process when commit transaction(primary key constraint)  
			insertCompensated(callerJournal.getTraceId(),
					callerJournal.getSequenceId(),callerJournal.getTxnId());
			result.setStatus(JournalStatus.NOTFOUND.getValue());
			result.setTxnStatus(TxnStatus.COMPENSATED.getValue());
		}
		return result;
	}
	
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void localStageCompensate(DtxStageJournal journal){
		Object[] args=null;
		ICompensatable compensatorBean=null;
		if (journal.isGeneric()){
			GenericRequest req= ClassSerializeUtil.deserialize(journal.getRequest());
			args=req.getArgs();
			Method method=localGenericExecutor.getDomainModelCache().get(journal.getDomain())
					.get(journal.getEndpoint()).getMethod();
			CompensatableTransactional ann=method.getAnnotation(CompensatableTransactional.class);
			compensatorBean=StringUtils.isNoneBlank(ann.compensator())
					?(ICompensatable) SpringContextHolder.getSpringBeanByName(ann.compensator())
					:SpringContextHolder.getContext().getBean(ann.compensatorClass());
		}else{
			args=ClassSerializeUtil.deserialize(journal.getRequest());
			compensatorBean = (ICompensatable) SpringContextHolder.getSpringBeanByName(journal.getEndpoint());
			if (compensatorBean==null) 
				compensatorBean=(ICompensatable) SpringContextHolder.getSpringBeanByType(journal.getDomain());
			if (compensatorBean==null)
				//TODO: do not need to compensate or wrong bean name?
				throw new RuntimeException("do not need to compensate or wrong bean name?");
		}		 
		compensatorBean.compensate(args);
		journalBroker.updateStatus(journal,null,TxnStatus.COMPENSATED);
		logger.debug("dtx# {} sequence# {}, stage {}-{} compensation complete",
				journal.getTxnId(),journal.getSequenceId(),journal.getDomain(),journal.getEndpoint());
	}
	
	public DtxStageJournal callerCompensate(ServiceCallerJournal journal){
		if (journal.isLocal()){
			DtxStageJournal stageJournal=insertCompensated(journal.getTraceId(),journal.getSequenceId(),journal.getTxnId());
			journalBroker.updateStatus(journal,null,TxnStatus.COMPENSATED);
			return stageJournal;
		}else{
			return remoteCallerJournal(journal);
		}
	}
	
	protected DtxStageJournal remoteCallerJournal(ServiceCallerJournal journal){
		ServiceCallerJournal param= new ServiceCallerJournal();
		param.setTraceId(journal.getTraceId());
		param.setSequenceId(journal.getSequenceId());
		param.setType(JournalType.SERVICE_CALLER.getValue());
		param.setTxnId(journal.getTxnId());
		String result= endpointInvoker.invokeDomain(journal.getDomain(), CommonConstants.DTX_STAGE_COMPENSATION_ENDPOINT,
				param,String.class);
		return ClassSerializeUtil.deserialize(result);
	}
	private DtxStageJournal insertCompensated(String traceId,int sequenceId,String dtxId){
		DtxStageJournal journal= new DtxStageJournal();
		journal.setTraceId(traceId);
		journal.setSequenceId(sequenceId);
		journal.setType(JournalType.DTX_STAGE.getValue());
		journal.setTxnId(dtxId);
		journal.setStatus(JournalStatus.FAILED.getValue());
		journal.setTxnStatus(TxnStatus.COMPENSATED.getValue());
		journalBroker.isolateInsert(journal);
		return journal;
	}
	
}
