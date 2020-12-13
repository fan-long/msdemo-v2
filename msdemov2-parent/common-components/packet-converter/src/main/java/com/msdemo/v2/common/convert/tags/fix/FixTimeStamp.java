package com.msdemo.v2.common.convert.tags.fix;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;

public class FixTimeStamp extends FixLengthTag {

	private boolean nullable;
	
	private String pattern ="yyyyMMddHHmmss";
	@Override
	public Object parseValue(String srcValue) {
		if (nullable && StringUtils.isBlank(srcValue)) return new Date();
		try {
			return new SimpleDateFormat(this.getPattern()).parse(srcValue);
		} catch (ParseException e) {
			throw new ConverterException(
					String.format(ConverterException.PARSER_FAILED, this.getName(), this.getPattern()));
		}
	}

	@Override
	public String formatValue(Object value) {
		if (nullable && value==null) return StringUtils.rightPad("", this.getPattern().length());
		return new SimpleDateFormat(this.getPattern()).format((Date)value);
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

}
