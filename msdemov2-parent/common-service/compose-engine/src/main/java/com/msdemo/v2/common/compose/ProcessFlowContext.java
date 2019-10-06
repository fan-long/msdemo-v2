package com.msdemo.v2.common.compose;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProcessFlowContext extends LinkedHashMap<String, Object> {

	/**
	 * TO support requirement of thread-safe, order and clone, do NOT use ConcurrentHashMap
	 */
	private static final long serialVersionUID = 5936203573860132603L;
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public static final String REQ_KEY="req";
	public static final String RESP_KEY="resp";
	public static final String EXCEPTION_FLOW_KEY="_exception_flow";
	private ProcessFlowException exception;
	
	public ProcessFlowContext(){
		super(8);
	}
	
	public Object getResponse(){
		return this.get(RESP_KEY);
	}
	
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		this.forEach( (k,v) ->{
			sb.append("[").append(k).append("] ").append(v).append(" ");
		});
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

	public ProcessFlowException getException() {
		return exception;
	}

	public void setException(ProcessFlowException exception) {
		this.exception = exception;
	}
}
