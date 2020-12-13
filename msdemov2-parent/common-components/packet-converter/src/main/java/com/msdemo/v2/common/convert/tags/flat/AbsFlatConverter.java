package com.msdemo.v2.common.convert.tags.flat;

import com.msdemo.v2.common.convert.definition.flat.FlatConverterContext;

public abstract class AbsFlatConverter<T>{
	private String name;
	abstract public T parse(FlatConverterContext context);
	abstract public String format(Object value);
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
