package com.msdemo.v2.common.dtx.compensation.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.msdemo.v2.async.spi.IAsyncTask;
import com.msdemo.v2.common.dtx.compensation.CdtxMainCompensator;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.journal.DtxJournalQueryService;
import com.msdemo.v2.common.dtx.journal.DtxMainJournal;
import com.msdemo.v2.common.journal.broker.InvocationJournalBroker;
import com.msdemo.v2.common.journal.model.TxnStatus;

public class CdtxRecompenseTask implements IAsyncTask<DtxMainJournal> {

	private static final Logger logger =LoggerFactory.getLogger(CdtxRecompenseTask.class);
	
	
	@Autowired
	DtxJournalQueryService journalQueryService;
	@Autowired
	InvocationJournalBroker journalBroker;
	@Autowired
	CdtxMainCompensator mainCompensator;
	
	
	@Override
	public List<DtxMainJournal> query() {
		List<DtxMainJournal> result= journalQueryService.queryRetrievable(CdtxMainCompensator.MAX_RETRY);
		logger.debug("dtx recompensate records: {}", result==null?0:result.size());
		return result;
	}

	@Override
	public void complete(List<DtxMainJournal> record) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fail(List<DtxMainJournal> record) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<DtxMainJournal> execute(List<DtxMainJournal> records) {
		for (DtxMainJournal journal:records){
			IDtxCoordinator.applyTrace(journal.getTxnId());
			journalBroker.updateStatus(journal, null, TxnStatus.COMPENSATING);
			mainCompensator.compensate(journal.getTxnId());
		}
		return records;
	}

	@Override
	public boolean isConsistent(){
		return false;
	}
}
