package com.msdemo.v2.common.convert.tags.flat;

import java.math.BigDecimal;

import com.msdemo.v2.common.convert.definition.flat.FlatConverterContext;

public class FlatBigDecimalTag extends AbsFlatConverter<BigDecimal> {

	@Override
	public BigDecimal parse(FlatConverterContext context) {
		return new BigDecimal(context.getAndForward());
	}

	@Override
	public String format(Object value) {
		return ((BigDecimal)value).toString();
	}

}
