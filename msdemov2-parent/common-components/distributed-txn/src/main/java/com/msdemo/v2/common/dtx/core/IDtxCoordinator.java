package com.msdemo.v2.common.dtx.core;

import org.slf4j.MDC;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.journal.DtxMainJournal;
import com.msdemo.v2.common.dtx.journal.DtxStageJournal;
import com.msdemo.v2.common.timing.spi.ITimerCallback;

public interface IDtxCoordinator extends ITimerCallback{

	static final String DTX_ID_PREFIX="DTX-";
	static final int DEFAULT_TIME_OUT=10;
	static final int DEFAULT_TIME_OUT_MILLIS=DEFAULT_TIME_OUT*1000;
	
	MainContext createMainContext(DtxMainJournal journal);
	void start(MainContext context);
	StageContext createStageContext(DtxStageJournal journal);
	void stagePrepare(StageContext context);
	void stageCommit(StageContext context);
	void stageRollback(StageContext context);
	void rollback(String traceId,String dtxId,String exceptionMsg);
	void commit(String dtxId, boolean isManaged,Object response);
	
	default String buildDtxId(){
		return DTX_ID_PREFIX.concat(TransContext.get().common.getTraceId());
	}
	static void applyTrace(String dtxId){
		MDC.put(CommonConstants.MDC_TRACE_ID, dtxId.substring(DTX_ID_PREFIX.length()));
	}
		
}
