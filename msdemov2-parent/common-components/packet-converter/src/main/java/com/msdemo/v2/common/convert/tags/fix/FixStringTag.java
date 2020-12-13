package com.msdemo.v2.common.convert.tags.fix;

public class FixStringTag extends FixLengthTag {

	@Override
	public Object parseValue(String srcValue) {
		return isTrim()?srcValue.trim():srcValue;
	}

	@Override
	public String formatValue(Object srcValue) {
		return (String)srcValue;
	}
}
