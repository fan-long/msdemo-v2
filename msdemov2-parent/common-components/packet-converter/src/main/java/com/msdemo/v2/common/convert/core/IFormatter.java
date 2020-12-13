package com.msdemo.v2.common.convert.core;

import java.util.Map;

public interface IFormatter {
	String format(Map<String,Object> src) throws ConverterException;
}
