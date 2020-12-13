package com.msdemo.v2.common.convert.core;

import java.util.Map;

public interface IConverter<T extends IConverterContext> extends IParser<T>, IFormatter {
	
	@Override
	default Object parse(T context) throws ConverterException{
		throw new ConverterException(
				String.format(ConverterException.METHOD_MISSING,"parse(ConverterContext)"));
	}
	
	@Override
	default String format(Map<String,Object> src) throws ConverterException{
		throw new ConverterException(
				String.format(ConverterException.METHOD_MISSING,"fromat(Map)"));
	}
}
