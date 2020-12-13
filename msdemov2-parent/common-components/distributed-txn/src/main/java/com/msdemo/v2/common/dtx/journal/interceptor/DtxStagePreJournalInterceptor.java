package com.msdemo.v2.common.dtx.journal.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.compensation.CompensatableTransactional;
import com.msdemo.v2.common.dtx.core.IDtxCoordinator;
import com.msdemo.v2.common.dtx.core.StageContext;
import com.msdemo.v2.common.dtx.journal.DtxStageJournal;
import com.msdemo.v2.common.generic.GenericRequest;
import com.msdemo.v2.common.generic.chain.interceptors.GenericPreJournalInterceptor;
import com.msdemo.v2.common.generic.executor.LocalGenericExecutor;
import com.msdemo.v2.common.invocation.invoker.InvocationModel;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.model.TxnStatus;

@Component
public class DtxStagePreJournalInterceptor extends GenericPreJournalInterceptor {

	@Autowired
	LocalGenericExecutor genericExecutor;

	@Autowired
	IDtxCoordinator dtxManager;
	
	@Override
    public void before(GenericServiceInterceptorModel model){
		
		if (!TransContext.isDistributedTransaction()) return;

		GenericRequest request = model.getRequest();
		InvocationModel im= genericExecutor.getDomainModelCache()
				.get(request.getDomain()).get(request.getEndpoint());
		if (im.getMethod().isAnnotationPresent(CompensatableTransactional.class)){
//			CompensatableTransactional transaction=im.getMethod().getAnnotation(CompensatableTransactional.class);
			StageContext context= dtxManager.createStageContext(new DtxStageJournal());
			DtxStageJournal journal= context.getJournal();		
			journal.setTxnStatus(TxnStatus.COMPLETED.getValue());
			journal.setTxnId(context.getDtxId());
			journal.setType(JournalType.DTX_STAGE.getValue());
			model.setJournal(journal);
			super.before(model);
		}
	}
}
