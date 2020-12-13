package com.msdemo.v2.common;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.utils.LogUtil;

public class ManagedThreadLocal<T> extends ThreadLocal<T>{
	
	private static final HashSet<ManagedThreadLocal<?>> holder = new HashSet<>();
	private static Logger logger = LoggerFactory.getLogger(ManagedThreadLocal.class);
	private String id;
	private Class<?> clz;
	private boolean autoInitial;
	
	public ManagedThreadLocal(){
		this(null,null,false);
	}
	
	public ManagedThreadLocal(Class<?> clz){
		this(null,clz,true);
	}
	public ManagedThreadLocal(String id,Class<?> clz){
		this(id,clz,true);
	}
	
	public ManagedThreadLocal(String id,Class<?> clz,boolean isAutoInitial){
		super();
		this.clz=clz;
		this.id= id!=null?id:this.getClass().getSimpleName()+this.hashCode();
		this.autoInitial=isAutoInitial;
		holder.add(this);
		logger.trace("add: {}", this.id);
	}
		
	public static void clean() {
		holder.forEach(mt -> {
			mt.remove();
		});
		if (logger.isDebugEnabled()){
			ArrayList<String> mts = new ArrayList<String>();
			holder.forEach(mt -> {
				mts.add(mt.getId());		        
			});
			logger.debug("cleaned: {} ", StringUtils.join(mts, ","));
		}
	}
	
	public String getId() {
		return id;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected  T initialValue(){	
		try {
			if (autoInitial){
				if (clz.equals(String.class))
					return (T)"";
				else
					return (T)(clz.newInstance());
			}
		} catch (Exception e) {
			LogUtil.exceptionLog(logger, e);
		}
		return null;
	}
	
}
