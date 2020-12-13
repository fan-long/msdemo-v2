package com.msdemo.v2.common.composite;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.composite.ProcessDefinition.TxnType;
import com.msdemo.v2.common.composite.flow.listeners.CompositeFlowListener;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.invocation.invoker.InvocationProxy;
import com.msdemo.v2.resource.script.spi.IScriptService;

public class CompositionContext extends LinkedHashMap<String, Object> {

	/**
	 * TO support requirement of thread-safe, order and clone, do NOT use ConcurrentHashMap
	 */
	private static final long serialVersionUID = 5936203573860132603L;
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private static Logger logger = LoggerFactory.getLogger(CompositionContext.class);
	
	private String processName;
	private Object req;
	private Object resp;
	
	private String exceptionFlow;
	private RuntimeException exception;
	private boolean txnTypeChanged=false;
	private boolean isChangeAllowed=true;
	private TxnType txnType;
	private InvocationProxy invocationProxy;
	private IScriptService scriptService;
	
		
	public CompositionContext(String processName,TxnType txnType){
		super(32);
		this.processName=processName;
		this.txnType=txnType;
	}
	public String getProcessName(){
		return this.processName;
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
	
	public TransContext.Context getTransContext(){
		return TransContext.get();
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.processName).append("\n");
		sb.append("[req] ").append(req).append("\n");
		this.forEach( (k,v) ->{
			sb.append("[").append(k).append("] ").append(v).append("\n");
		});
		sb.append("[resp] ").append(resp).append("\n");
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getFlowResult(String key){
//		lock.readLock().lock();
//		try{
			return (T) super.get(key);
//		}finally{
//			lock.readLock().unlock();
//		}
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
	public InvocationProxy getInvocationProxy() {
		return invocationProxy;
	}
	public void setInvocationProxy(InvocationProxy invocationProxy) {
		this.invocationProxy = invocationProxy;
	}
	public IScriptService getScriptService() {
		return scriptService;
	}
	public void setScriptService(IScriptService scriptService) {
		this.scriptService = scriptService;
	}
	
	public CompositeFlowListener getDefaultListener(){
		return CompositionFactory.get(processName).defaultListener;
	}
}
