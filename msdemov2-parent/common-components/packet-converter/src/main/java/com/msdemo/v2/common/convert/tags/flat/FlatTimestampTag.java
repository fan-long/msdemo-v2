package com.msdemo.v2.common.convert.tags.flat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.msdemo.v2.common.convert.definition.flat.FlatConverterContext;

public class FlatTimestampTag extends AbsFlatConverter<Date> {

	private SimpleDateFormat pattern;
	
	@Override
	public Date parse(FlatConverterContext context) {		
		try {
			return  pattern.parse(context.getAndForward());
		} catch (ParseException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public String format(Object value) {
		return pattern.format((Date)value);
	}

	public void setPattern(String pattern){
		this.pattern=new SimpleDateFormat(pattern);
	}
}
