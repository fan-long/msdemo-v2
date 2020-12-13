package com.msdemo.v2.common.dtx.core;

import com.msdemo.v2.common.ManagedThreadLocal;
import com.msdemo.v2.common.dtx.journal.DtxStageJournal;

public class StageContext extends AbsDtxContext<DtxStageJournal>{

	private static final ManagedThreadLocal<StageContext> ContextHolder=
			new ManagedThreadLocal<>(StageContext.class.getName(),StageContext.class,false);
	public static StageContext get(){	return ContextHolder.get();}
	public static void set(StageContext context){ContextHolder.set(context);}
	
	public StageContext(){}
	public StageContext(DtxStageJournal journal) {
		super(journal);
	}
	
	
	private DtxStageJournal journal;
	
	public DtxStageJournal getJournal() {
		return journal;
	}
	
	@Override
	public void setJournal(DtxStageJournal journal) {
		this.journal = journal;
	}
		 
	@Override
	public void clean() {
		ContextHolder.remove();
	}
}
