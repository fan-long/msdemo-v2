package com.msdemo.v2.common.convert.core;

public interface IParser<T extends IConverterContext> {
	Object parse(T context) throws ConverterException;
}
