package com.msdemo.v2.resource.script.store;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.msdemo.v2.common.core.IScriptStore;

public class RedisScriptStore implements IScriptStore {

	@Autowired
	RedisTemplate<String,String> redis;
	
	private static final String SCRIPT_ROOT="script";
	
	@Override
	public void create(String id, String context) {
		redis.boundHashOps(SCRIPT_ROOT).put(id,context);
	}

	@Override
	public String load(String id) {
		return (String) redis.boundHashOps(SCRIPT_ROOT).get(id);
	}

	@Override
	public void replace(String id, String newContent) {
		create(id,newContent);
	}

	@Override
	public Map<String, String> loadAll() {
		Map<Object,Object> h=redis.boundHashOps(SCRIPT_ROOT).entries();
		Map<String,String> result= new HashMap<>(h.size(),1);
		h.forEach( (k,v) -> result.put(k.toString(), v.toString()));
		return result;
	}

}
