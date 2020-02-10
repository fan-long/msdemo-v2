package com.msdemo.v2.resource.script.groovy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class ImmediateGroovyScript  {

	private static final String GROOVY = "groovy";
	private static ScriptEngine engine = new ScriptEngineManager().getEngineByName(GROOVY);
    private static GroovyShell groovyShell = new GroovyShell();
    
    private static ConcurrentHashMap<String,Script> CachedScript=
    		new ConcurrentHashMap<>();

	private Script cacheScript(String id,String scriptContent, Binding binding){
		Script script= groovyShell.parse(scriptContent);
		script.setBinding(binding);
		CachedScript.put(id, script);
		return script;
	}
	
	public Object execute(String id,String script,Map<String,?> params){
		if (!CachedScript.containsKey(id)){
			Binding binding = new Binding();
			params.forEach( (k,v) -> 
				binding.setProperty(k, v)
			);
            cacheScript(id,script,binding);
		}
		return CachedScript.get(id).run();
	}
	

	public Object executeScript(String script, Map<String,?> params){
		try {
			Bindings bindings = engine.createBindings();
            bindings.putAll(params);
			return engine.eval(script,bindings);
		} catch (ScriptException e) {
			throw new RuntimeException(e);

		}
	}

}
