package com.msdemo.v2.common.dtx.journal;

import org.apache.commons.lang3.tuple.MutablePair;

import com.msdemo.v2.common.journal.model.AbsInvocationJournal;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.spi.IJournalEntityType;

public class DtxJournalEntityType {

	public static class DTX_MAIN implements IJournalEntityType{
		@Override
		public MutablePair<JournalType, AbsInvocationJournal<?>> getEntityType() {
			return new MutablePair<JournalType, AbsInvocationJournal<?>>(JournalType.DTX_MAIN, new DtxMainJournal());
		}		
	}
	
	public static class DTX_STAGE implements IJournalEntityType{
		@Override
		public MutablePair<JournalType, AbsInvocationJournal<?>> getEntityType() {
			return new MutablePair<JournalType, AbsInvocationJournal<?>>(JournalType.DTX_STAGE, new DtxStageJournal());
		}		
	}
	
	public static class DTX_COMPENSATION implements IJournalEntityType{
		@Override
		public MutablePair<JournalType, AbsInvocationJournal<?>> getEntityType() {
			return new MutablePair<JournalType, AbsInvocationJournal<?>>(JournalType.DTX_COMPENSATION, new DtxMainJournal());
		}		
	}
}
