package com.msdemo.v2.common.dtx.compensation.task;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.msdemo.v2.async.spi.IAsyncTask;
import com.msdemo.v2.common.dtx.compensation.CdtxMainCompensator;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.journal.DtxJournalQueryService;
import com.msdemo.v2.common.dtx.journal.DtxMainJournal;
import com.msdemo.v2.common.journal.model.JournalStatus;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;

/**
 * monitor for 2 expiration type
 * 1. expired main journal, trigger compensation, same as transaction timeout by TimingExecution listener 
 * 2. expired compensation journal, update status for retrievable task
 * @author LONGFAN
 *
 */

public class CdtxExpirationMonitorTask implements IAsyncTask<DtxMainJournal> {
	
	private static final Logger logger =LoggerFactory.getLogger(CdtxExpirationMonitorTask.class);
	@Autowired
	DtxJournalQueryService journalQueryService;
	
	@Autowired
	CdtxMainCompensator mainCompensator;
	
	
	@Override
	public List<DtxMainJournal> query() {
		List<DtxMainJournal> result=new ArrayList<>();
		List<DtxMainJournal> expiredMain=journalQueryService.queryExpired(JournalType.DTX_MAIN,JournalStatus.RUNNING,null);
		List<DtxMainJournal> expiredCompensate=journalQueryService.queryExpired(JournalType.DTX_COMPENSATION, null, TxnStatus.COMPENSATING);
		if (expiredMain!=null) result.addAll(expiredMain);
		if (expiredCompensate!=null) result.addAll(expiredCompensate);
		
		logger.debug("dtx expiration, main: {}, compensate: {}", expiredMain==null?0:expiredMain.size(),
					expiredCompensate==null?0:expiredCompensate.size());
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
		List<DtxMainJournal> result=new ArrayList<>();
		for (DtxMainJournal journal:records){
			if (journal.getType()==JournalType.DTX_MAIN.getValue()){
				try {
					IDtxCoordinator.applyTrace(journal.getTxnId());
					mainCompensator.markRollback(journal.getTraceId(),
							journal.getTxnId(),"by expiration monitor");
				} catch (Exception e) {
					logger.warn("mark dtx# {} failed, msg: {}",journal.getTxnId(), e.getMessage());
					continue;
				}
				result.add(journal);
				mainCompensator.compensate(journal.getTxnId());
			}else{
				journal.setCompensationJournal(journal);
				mainCompensator.updateMainStatus(journal, false);
				result.add(journal);
			}
		}
		return result;
	}

	@Override
	public boolean isConsistent(){
		return false;
	}
}
