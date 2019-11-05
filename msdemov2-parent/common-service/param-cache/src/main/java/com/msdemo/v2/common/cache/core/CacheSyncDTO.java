package com.msdemo.v2.common.cache.core;


public class CacheSyncDTO {

	private String cacheKey;
	private Action action;
	private Object[] value;
		
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

	public static enum Action{
		INSERT, DELETE, UPDATE;
		public static Action fromMethod(String methodName){
			String name = methodName.toUpperCase();
			if (name.startsWith(INSERT.toString()))
				return INSERT;
			else if (name.startsWith(UPDATE.toString()))
				return UPDATE;
			else if (name.startsWith(DELETE.toString()))
				return DELETE;
			throw new RuntimeException("incorrect action name: " + methodName);
		}
	}
}
