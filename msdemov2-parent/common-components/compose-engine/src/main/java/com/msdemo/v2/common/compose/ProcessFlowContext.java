package com.msdemo.v2.common.compose;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.compose.ProcessFlow.TxnType;

public class ProcessFlowContext extends LinkedHashMap<String, Object> {

	/**
	 * TO support requirement of thread-safe, order and clone, do NOT use ConcurrentHashMap
	 */
	private static final long serialVersionUID = 5936203573860132603L;
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private static Logger logger = LoggerFactory.getLogger(ProcessFlowContext.class);
	
	private String processFlowName;
	private Object req;
	private Object resp;
	
	private String exceptionFlow;
	private RuntimeException exception;
	private boolean txnTypeChanged=false;
	private boolean isChangeAllowed=true;
	private TxnType txnType;
	
	
	public ProcessFlowContext(String processFlowName,TxnType txnType){
		super(32);
		this.processFlowName=processFlowName;
		this.txnType=txnType;
	}
	public String getProcessFlowName(){
		return this.processFlowName;
	}
	public void setResp(Object resp){
		this.resp=resp;
	}
	public Object getResp(){
		return this.resp;
	}
	
	public TxnType getTxnType(){
		return this.txnType;
	}
	
	public void setTxnType(TxnType txnType){
		if (this.txnType.equals(txnType)) return;
		if (isChangeAllowed && !txnTypeChanged){
			this.txnType=txnType;
			txnTypeChanged=true;
			isChangeAllowed=false;
			logger.info("txnType has been assigned to {}",txnType);
		}else{
			throw new RuntimeException("Transaction Type had been assigned to "+getTxnType());
		}
	}
	
	public boolean isTxnTypeChanged(){
		return txnTypeChanged;
	}
	public void removeChangeFlag(){
		txnTypeChanged=false;
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.processFlowName).append("\n");
		sb.append("[req] ").append(req).append("\n");
		this.forEach( (k,v) ->{
			sb.append("[").append(k).append("] ").append(v).append("\n");
		});
		sb.append("[resp] ").append(resp).append("\n");
		return sb.toString();
	}
	
	@Override
	public Object get(Object key){
		lock.readLock().lock();
		try{
			return super.get(key);
		}finally{
			lock.readLock().unlock();
		}
	}
	@Override
	public Object put(String key,Object value){
		lock.writeLock().lock();
		try{
			return super.put(key,value);
		}finally{
			lock.writeLock().unlock();
		}
	}

	public RuntimeException getException() {
		return exception;
	}

	public void setException(RuntimeException exception) {
		this.exception = exception;
	}
	public Object getReq() {
		return req;
	}
	public void setReq(Object req) {
		this.req = req;
	}
	public String getExceptionFlow() {
		return exceptionFlow;
	}
	public void setExceptionFlow(String exceptionFlow) {
		this.exceptionFlow = exceptionFlow;
	}
}
