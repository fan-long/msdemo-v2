package com.msdemo.v2.common.convert.tags.fix;

public class FixInteger extends FixLengthTag {

	@Override
	public Object parseValue(String srcValue) {
		return Integer.parseInt(srcValue.trim());
	}

	@Override
	public String formatValue(Object value) {
		return value.toString();
	}

}
