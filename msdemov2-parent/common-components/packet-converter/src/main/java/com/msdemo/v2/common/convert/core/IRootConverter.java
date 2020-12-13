package com.msdemo.v2.common.convert.core;


public interface IRootConverter<T extends IConverterContext> extends IConverter<T> {

	T marshal(byte[] src);
	String unmarshal(Object src);	
}
