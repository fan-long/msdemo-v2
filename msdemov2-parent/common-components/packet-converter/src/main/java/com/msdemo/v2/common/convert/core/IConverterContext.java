package com.msdemo.v2.common.convert.core;

public interface IConverterContext {

	public void setDto(Object o);
	public <T> T getDto();
}
