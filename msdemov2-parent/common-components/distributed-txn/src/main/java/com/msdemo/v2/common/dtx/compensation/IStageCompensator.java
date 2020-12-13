package com.msdemo.v2.common.dtx.compensation;

import com.msdemo.v2.common.dtx.journal.DtxStageJournal;
import com.msdemo.v2.common.invocation.journal.ServiceCallerJournal;

public interface IStageCompensator {

	public DtxStageJournal remoteStageCompensate(ServiceCallerJournal callerJournal);
	public void localStageCompensate(DtxStageJournal journal);
	public DtxStageJournal callerCompensate(ServiceCallerJournal journal);

}
