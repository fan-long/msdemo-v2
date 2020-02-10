package com.msdemo.v2.common.core;

import java.util.Map;

public interface IScriptStore {

	Map<String,String> loadAll();
	void create(String id, String context);
	String load(String id);
	void replace(String id, String newContent);
}
