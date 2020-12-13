package com.msdemo.v2.common.dtx.journal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.journal.broker.InvocationJournalBroker;
import com.msdemo.v2.common.journal.broker.JournalException;
import com.msdemo.v2.common.journal.model.AbsInvocationJournal;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;
import com.msdemo.v2.common.journal.repo.DefaultJournal;
import com.msdemo.v2.common.journal.spi.IJournalAgent;

@Component
public class DtxJournalQueryService {

	@Autowired
	InvocationJournalBroker journalBroker;
	
	private static Set<IJournalAgent> dtxJournalAgents= null;
	
	public DtxMainJournal queryMainJournal(String dtxId){
		List<AbsInvocationJournal<?>> journals= journalBroker.getConfig().getActiveAgent()
				.get(JournalType.DTX_MAIN.getValue())
				.queryByTxnId(dtxId,JournalType.DTX_MAIN);
		if (journals==null || journals.size()==0) 
			return null;
		else if (journals.size()>1)
			throw new JournalException(JournalException.Duplicate);
		return (DtxMainJournal)journals.get(0);
	}
	public DtxStageJournal queryStageJournal(String dtxId,int sequenceId){
		List<AbsInvocationJournal<?>> journals= journalBroker.getConfig().getActiveAgent()
				.get(JournalType.DTX_STAGE.getValue())
				.queryByTxnId(dtxId,JournalType.DTX_STAGE);
		if (journals==null || journals.size()==0) return null;
		else 		
			return (DtxStageJournal)journals.get(0);

}
	
	public TxnStatus queryStatus(String dtxId) {
		DtxMainJournal journal= queryMainJournal(dtxId);
		return convertTxnStatus(journal);
	}
	public TxnStatus convertTxnStatus(DefaultJournal journal){
		if (journal==null)
			return TxnStatus.NOTFOUND;
		else
			return TxnStatus.ofValue(journal.getTxnStatus());
	}
	
	//FIXME: fetch size
	public List<DtxMainJournal> queryExpired(JournalType type, JournalStatus status,TxnStatus txnStatus){
		List<AbsInvocationJournal<?>> list=journalBroker.getConfig().getActiveAgent().get(JournalType.DTX_COMPENSATION.getValue())
			.queryExpired(type,status,txnStatus);
		if (list!=null && list.size()>0){
			List<DtxMainJournal> result = new ArrayList<>(list.size());
			for (AbsInvocationJournal<?> journal: list){
				result.add((DtxMainJournal)journal);
			}
			return result;
		}
		return null;
	}
	
	//FIXME: fetch size
	public List<DtxMainJournal> queryRetrievable(int maxRetry){
		List<AbsInvocationJournal<?>> list=journalBroker.getConfig().getActiveAgent().get(JournalType.DTX_COMPENSATION.getValue())
			.queryRetrievable(JournalType.DTX_COMPENSATION,TxnStatus.FAILED,maxRetry);
		if (list!=null && list.size()>0){
			List<DtxMainJournal> result = new ArrayList<>(list.size());
			for (AbsInvocationJournal<?> journal: list){
				result.add((DtxMainJournal)journal);
			}
			return result;
		}
		return null;
	}
	
	public DtxMainJournal queryTxnJournals(String txnId){
		if (dtxJournalAgents==null){
			dtxJournalAgents=new HashSet<>();
			dtxJournalAgents.add(journalBroker.getConfig()
					.getActiveAgent().get(JournalType.DTX_MAIN.getValue()));
			dtxJournalAgents.add(journalBroker.getConfig()
					.getActiveAgent().get(JournalType.SERVICE_CALLER.getValue()));
			dtxJournalAgents.add(journalBroker.getConfig()
					.getActiveAgent().get(JournalType.DTX_STAGE.getValue()));
			dtxJournalAgents.add(journalBroker.getConfig()
					.getActiveAgent().get(JournalType.DTX_COMPENSATION.getValue()));
		}
		Map<Integer,Map<Integer,AbsInvocationJournal<?>>> result=new HashMap<>();
		for (IJournalAgent agent: dtxJournalAgents){
			List<AbsInvocationJournal<?>> records=agent.queryByTxnId(txnId);
			if (records!=null) {
				records.forEach( journal -> {
					if (!result.containsKey(journal.getType())) 
						result.put(journal.getType(), new HashMap<Integer,AbsInvocationJournal<?>>());
					result.get(journal.getType()).put(journal.getSequenceId(),journal);	
				});
			}
		}
		DtxMainJournal mainJournal= (DtxMainJournal) result.get(JournalType.DTX_MAIN.getValue()).get(0);
		if (mainJournal==null){
			mainJournal= new DtxMainJournal();
			mainJournal.setStatus(JournalStatus.NOTFOUND.getValue());
		}
		List<AbsInvocationJournal<?>> stageJournals = new ArrayList<>(4);
		if (result.containsKey(JournalType.SERVICE_CALLER.getValue())){
			for (int sequenceId: result.get(JournalType.SERVICE_CALLER.getValue()).keySet()){
				if (result.containsKey(JournalType.DTX_STAGE.getValue())){
					AbsInvocationJournal<?> journal=
						result.get(JournalType.DTX_STAGE.getValue()).get(sequenceId);
					if (journal!=null){
						//both caller and dtx journal found, use dtx
						stageJournals.add(journal);
						result.get(JournalType.DTX_STAGE.getValue()).remove(sequenceId);
						continue;
					}
				}
				//no dtx found, use caller
				stageJournals.add(result.get(JournalType.SERVICE_CALLER.getValue()).get(sequenceId));					
			}
		}
		//add the rest of dtx without caller
		if (result.containsKey(JournalType.DTX_STAGE.getValue()))
			stageJournals.addAll(result.get(JournalType.DTX_STAGE.getValue()).values());
		stageJournals.sort((a,b) -> b.getSequenceId().compareTo(a.getSequenceId()));
		mainJournal.setSortedStageJournals(stageJournals);
		mainJournal.setCompensationJournal((DtxMainJournal) result.get(JournalType.DTX_COMPENSATION.getValue()).get(0));
		return mainJournal;
	}
}
