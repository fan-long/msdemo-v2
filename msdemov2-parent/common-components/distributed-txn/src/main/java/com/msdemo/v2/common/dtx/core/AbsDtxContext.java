package com.msdemo.v2.common.dtx.core;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;

import com.msdemo.v2.common.dtx.compensation.CompensatableTransactional;
import com.msdemo.v2.common.journal.model.AbsInvocationJournal;

public abstract class AbsDtxContext<T extends AbsInvocationJournal<?>> {

	private boolean outSourcing;

	private String dtxId;
	private ProceedingJoinPoint joinPoint;
	private CompensatableTransactional cdtxConfig;
	private Set<String> locks;
	private Object response;
	
	public AbsDtxContext(){}
	public AbsDtxContext(T journal){ 
		this.outSourcing=true;
		setJournal(journal);
	}
	
	abstract void setJournal(T journal);
	
	public boolean isRunning(){
		return cdtxConfig != null;
	}
	public String getDtxId() {
		return dtxId;
	}

	public void setDtxId(String dtxId) {
		this.dtxId = dtxId;
	}
	public Set<String> getLocks() {
		return locks;
	}
	public void setLocks(Set<String> locks) {
		this.locks = locks;
	}

	public boolean isOutSourcing() {
		return outSourcing;
	}
	public ProceedingJoinPoint getJoinPoint() {
		return joinPoint;
	}
	public void setJoinPoint(ProceedingJoinPoint joinPoint) {
		this.joinPoint = joinPoint;
	}
	public CompensatableTransactional getCdtxConfig() {
		return cdtxConfig;
	}
	public void setCdtxConfig(CompensatableTransactional cdtxConfig) {
		this.cdtxConfig = cdtxConfig;
	}
	public Object getResponse() {
		return response;
	}
	public void setResponse(Object response) {
		this.response = response;
	}

	abstract public void clean();
}
