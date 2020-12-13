package com.msdemo.v2.common.composite.flow.listeners;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.flow.AbstractFlow;
import com.msdemo.v2.common.composite.flow.AbstractInvokerFlow;
import com.msdemo.v2.common.composite.flow.GenericFlow;
import com.msdemo.v2.common.composite.flow.ScriptFlow;
import com.msdemo.v2.common.composite.flow.SimpleFlow;
import com.msdemo.v2.common.composite.journal.FlowJournal;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.generic.GenericServiceConfig;
import com.msdemo.v2.common.generic.executor.LocalGenericExecutor;
import com.msdemo.v2.common.journal.broker.InvocationJournalBroker;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.common.sequence.increment.IIncrementIdGenerator;
import com.msdemo.v2.common.utils.LogUtil;

@Component
public class FlowJournalListener implements IFlowListener {

	private static final String ETA_KEY="__ETA";
	private static final String FLOW_JOURNAL="__JOURNAL";
	
	@Autowired
	InvocationJournalBroker journalBroker;
	
	@Autowired
	GenericServiceConfig genericConfig;
	
	@Autowired
	LocalGenericExecutor executor;
	
	@Autowired
	IIncrementIdGenerator stageIdGenerator;
	
	@Override
	public void beforeFlow(AbstractFlow flow, CompositionContext context) {
		context.put(ETA_KEY, Instant.now().toEpochMilli());		
	}

	private static final Logger logger =LoggerFactory.getLogger(FlowJournalListener.class);

	@Override
	public void beforeInvoke(AbstractFlow flow, CompositionContext context,Object[] args){
		if (!TransContext.isDistributedTransaction() || !flow.isJournalEnabled()) return;
		FlowJournal journal=null;
		if (flow instanceof GenericFlow){
			String domain=((GenericFlow) flow).getDomain();
			String endpoint=((GenericFlow) flow).getEndpoint();
			if (genericConfig.getConfig(domain, endpoint).getJournal().isEnabled())
				journal= buildJournal(flow.getName(),domain,endpoint,true,executor.isLocalDomain(domain),args);
		}else if (flow instanceof SimpleFlow){
			String domain=executor.getClassDomainCache().get(((SimpleFlow)flow).getClassName());
			if (domain==null){ 
				logger.debug("skip flow [{}], class: {}",flow.getName(),((AbstractInvokerFlow)flow).getClassName());
//				throw new CompositionException(((AbstractInvokerFlow)flow).getClassName()+
//					" is not a domain service, DTX not supported.");
				return;
			}
			String endpoint=(((AbstractInvokerFlow)flow).getMethodName());
			if (genericConfig.getConfig(domain, endpoint).getJournal().isEnabled())
				journal= buildJournal(flow.getName(),domain,endpoint,false,executor.isLocalDomain(domain),args);
		}else if (flow instanceof ScriptFlow){
			if (((ScriptFlow) flow).isTransactional()){
				journal = new FlowJournal();
				journal.setLocal(true);
			}
		}else{
			//FIXME: async and parallel flow, TransContext should be passed to async thread
			journal=buildJournal(flow.getName(),"","",false,true,args);
		}
		if (journal!=null){
			if (journalBroker.getConfig().isAsync()){
				//add stage count for transactional invocation
				long stageIndex=stageIdGenerator.nextIdByGroup(TransContext.get().common.getTxn().getDtxId());
				logger.debug("dtx# {}: async journal stage {} started",TransContext.get().common.getTxn().getDtxId(),stageIndex);
			}
			if(!journal.isLocal()) {
				//add stage journal for remotely transactional invocation
				journal.setSequenceId(TransContext.get().common.getSequenceId());
				journalBroker.insert(journal);
				context.put(FLOW_JOURNAL, journal);
			}
		}
	}
	
	protected FlowJournal buildJournal(String flowName,String domain,String endpoint
			,boolean isGeneric,boolean isLocal,Object[] args){
			FlowJournal journal = new FlowJournal();
			journal.setTraceId(TransContext.get().common.getTraceId());
			journal.setType(JournalType.SERVICE_CALLER.getValue());
			journal.setStatus(JournalStatus.RUNNING.getValue());
			journal.setTxnStatus(TxnStatus.RUNNING.getValue());
			journal.setGeneric(isGeneric);
			journal.setFlowName(flowName);
			journal.setDomain(domain);
			journal.setEndpoint(endpoint);
			journal.setLocal(isLocal);
			journal.setTxnId(TransContext.get().common.getTxn().getDtxId());	
			journal.setReqObject(args);
			return journal;
	}
	
	
	@Override
	public void afterFlow(AbstractFlow flow, CompositionContext context) {
		LogUtil.cost(logger, (Long)context.get(ETA_KEY), flow.getName());		
		if (context.containsKey(FLOW_JOURNAL)){
			FlowJournal journal=(FlowJournal)context.get(FLOW_JOURNAL);
			FlowJournal updated = new FlowJournal();
			updated.setTraceId(journal.getTraceId());
			updated.setSequenceId(journal.getSequenceId());
			updated.setType(JournalType.SERVICE_CALLER.getValue());
			updated.setRespObject(context.get(flow.getName()));
			updated.setElapsed(Instant.now().toEpochMilli()-(Long)context.get(ETA_KEY));
			updated.setStatus(JournalStatus.COMPLETED.getValue());
			updated.setTxnStatus(TxnStatus.COMPLETED.getValue());
			journalBroker.update(updated);
			context.remove(FLOW_JOURNAL);
		}
	}

}
