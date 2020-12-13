package com.msdemo.v2.common.convert.tags.fix;

import java.math.BigDecimal;

public class FixBigDecimal extends FixLengthTag {

	@Override
	public Object parseValue(String srcValue) {
		return new BigDecimal(srcValue);
	}

	@Override
	public String formatValue(Object value) {
		return ((BigDecimal)value).toString();
	}

}
