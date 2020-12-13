package com.msdemo.v2.common.dtx.core;

import com.msdemo.v2.common.ManagedThreadLocal;
import com.msdemo.v2.common.dtx.journal.DtxMainJournal;

public class MainContext extends AbsDtxContext<DtxMainJournal>{
	
	private static final ManagedThreadLocal<MainContext> ContextHolder=
			new ManagedThreadLocal<>(MainContext.class.getName(),MainContext.class,false);
	
	public MainContext() {}
	public MainContext(DtxMainJournal journal) {
		super(journal);
	}
	public static MainContext get(){	return ContextHolder.get();}
	public static void set(MainContext context){ContextHolder.set(context);}
	 
	private DtxMainJournal journal;
	
	public DtxMainJournal getJournal() {
		return journal;
	}
	public void setJournal(DtxMainJournal journal) {
		this.journal = journal;
	}
	@Override
	public void clean() {
		ContextHolder.remove();
	}


}
