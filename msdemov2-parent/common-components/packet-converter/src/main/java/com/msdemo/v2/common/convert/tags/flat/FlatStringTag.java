package com.msdemo.v2.common.convert.tags.flat;

import com.msdemo.v2.common.convert.definition.flat.FlatConverterContext;

public class FlatStringTag extends AbsFlatConverter<String> {

	@Override
	public String parse(FlatConverterContext context) {
		return context.getAndForward();
	}

	@Override
	public String format(Object value) {
		return (String) value;
	}

}
