package com.msdemo.v2.common.packet;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.msdemo.v2.common.convert.core.ConverterFactory;
import com.msdemo.v2.common.convert.core.IConverterContext;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.definition.flat.FlatConverterContext;

@Component
public class FlatPacketConverter {
	
	@Resource(name="flatConverterFactory")
	ConverterFactory flatConverterFactory;
	
	@SuppressWarnings("unchecked")
	public <T> T marshal(String transCode,String inputMsg) {
		T result = null;
		IRootConverter<?> converter = flatConverterFactory.getInstance(transCode);
		IConverterContext context = converter.marshal(inputMsg.getBytes());
		if (result instanceof FlatConverterContext) {
			return (T) context;
		} else
			return (T) context.getDto();
	}
	
	public String unmarshal(String transCode,Object dto) {
		IRootConverter<?> converter = flatConverterFactory.getInstance(transCode);
		return converter.unmarshal(dto);
	}
}
