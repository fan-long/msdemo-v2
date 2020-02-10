package com.msdemo.v2.resource.script.groovy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.msdemo.v2.common.core.IScriptService;
import com.msdemo.v2.common.core.IScriptStore;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;


public class GroovyCompiledScriptService implements IScriptService {

	@Autowired
	IScriptStore store;
	
	private static final Logger logger = LoggerFactory.getLogger(GroovyCompiledScriptService.class);
	GroovyClassLoader gcl = new GroovyClassLoader();
	private ConcurrentHashMap<String,GroovyObject> CachedClass= new ConcurrentHashMap<>();
	private static final String METHOD_NAME="main";
	
	public void cacheScript(String scriptId,String scriptContent){
		Class<?> clz=gcl.parseClass(scriptContent);
		try {
			CachedClass.put(scriptId,(GroovyObject) clz.newInstance());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		logger.info("cached groovy script# {}",scriptId);
	}
	
	@Override
	public Object execute(String scriptId, Map<String, ?> context) {
		return CachedClass.get(scriptId).invokeMethod(METHOD_NAME, context);
	}

	@Override
	public Object executeScript(String scriptContent, Map<String, ?> context) {
		String id= String.valueOf(scriptContent.hashCode());
		logger.debug("execute groovy script, id: {}",id);
		if (!CachedClass.containsKey(id))		
			cacheScript(id,scriptContent);
		return execute(id,context);			
	}

	@Override
	public Map<String, ?> scriptCache() {
		return CachedClass;
	}

	@Override
	public IScriptStore scriptStore() {
		return store;
	}

}
