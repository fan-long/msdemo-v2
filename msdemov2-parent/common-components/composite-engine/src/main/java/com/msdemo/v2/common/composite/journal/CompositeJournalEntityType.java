package com.msdemo.v2.common.composite.journal;

import org.apache.commons.lang3.tuple.MutablePair;

import com.msdemo.v2.common.journal.model.AbsInvocationJournal;
import com.msdemo.v2.common.journal.model.JournalType;
import com.msdemo.v2.common.journal.spi.IJournalEntityType;

public class CompositeJournalEntityType {

	public static class PROCESS implements IJournalEntityType{
		@Override
		public MutablePair<JournalType, AbsInvocationJournal<?>> getEntityType() {
			return new MutablePair<JournalType, AbsInvocationJournal<?>>(JournalType.PROCESS, new ProcessJournal());
		}		
	}
	
	public static class FLOW implements IJournalEntityType{
		@Override
		public MutablePair<JournalType, AbsInvocationJournal<?>> getEntityType() {
			return new MutablePair<JournalType, AbsInvocationJournal<?>>(JournalType.SERVICE_CALLER, new FlowJournal());
		}		
	}
	
	
}
