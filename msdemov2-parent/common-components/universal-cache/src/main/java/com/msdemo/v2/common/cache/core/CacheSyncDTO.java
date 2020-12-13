package com.msdemo.v2.common.cache.core;

import java.io.Serializable;

import org.aspectj.lang.ProceedingJoinPoint;

public class CacheSyncDTO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6736670361965132780L;
	
	private String cacheKey;
	private Action action;
	private Object[] value;
	private transient ProceedingJoinPoint pjd;	
	
	public String getCacheKey() {
		return cacheKey;
	}
	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}
	public Object[] getValue() {
		return value;
	}
	public void setValue(Object[] value) {
		this.value = value;
	}
	public Action getAction() {
		return action;
	}
	public void setAction(Action action) {
		this.action = action;
	}

	public ProceedingJoinPoint getPjd() {
		return pjd;
	}
	public void setPjd(ProceedingJoinPoint pjd) {
		this.pjd = pjd;
	}

	public static enum Action{
		CREATE,INSERT, DELETE, UPDATE;
		public static Action fromMethod(String methodName){
			String name = methodName.toUpperCase();
			if (name.startsWith(CREATE.toString()) || name.startsWith(INSERT.toString()))
				return INSERT;
			else if (name.startsWith(UPDATE.toString()))
				return UPDATE;
			else if (name.startsWith(DELETE.toString()))
				return DELETE;
			throw new RuntimeException("incorrect action name: " + methodName);
		}
	}
}
