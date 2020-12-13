package com.msdemo.v2.common.composite.param;

import java.util.LinkedHashMap;

public class ProcessResultMap extends LinkedHashMap<String, Object> implements INewInstance {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3056451723487414488L;

	@Override
	public Object newInstance() {
		return new LinkedHashMap<>();
	}

}
