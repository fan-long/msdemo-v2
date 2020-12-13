package com.msdemo.v2.common.convert.tags.fix;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.definition.fix.AbsFixContainer;
import com.msdemo.v2.common.convert.definition.fix.AbsFixConverter;
import com.msdemo.v2.common.convert.definition.fix.FixConverterContext;

@SuppressWarnings("unchecked")
public class SegmentTag extends AbsFixContainer implements IRootConverter<FixConverterContext> {
		
	@Override
	public String format(Map<String,Object> src) throws ConverterException{
		StringBuilder sb= new StringBuilder();
		for (AbsFixConverter converter : this.getChildren()) {
			if (logger.isTraceEnabled()) 
				logger.trace("{}:{}",converter.getName(),src.get(converter.getName()));
			sb.append(converter.format(
					StringUtils.isNotEmpty(this.getName())?(Map<String,Object>)src.get(this.getName()):src));
		}
		return sb.toString();
	}

	@Override
	public FixConverterContext marshal(byte[] src) {
		return null;
	}

	@Override
	public String unmarshal(Object src) {
		return null;
	}
}
