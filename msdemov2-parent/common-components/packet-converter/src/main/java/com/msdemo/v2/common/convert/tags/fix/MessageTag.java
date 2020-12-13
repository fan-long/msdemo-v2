package com.msdemo.v2.common.convert.tags.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.definition.fix.AbsFixContainer;
import com.msdemo.v2.common.convert.definition.fix.AbsFixConverter;
import com.msdemo.v2.common.convert.definition.fix.FixConverterContext;
import com.msdemo.v2.common.utils.ValueCopyUtil;

@SuppressWarnings("unchecked")
public class MessageTag extends AbsFixContainer implements IRootConverter<FixConverterContext> {

	
	private String bodyLengthField;
	
	

	public String getBodyLengthField() {
		return bodyLengthField;
	}

	public void setBodyLengthField(String bodyLengthField) {
		this.bodyLengthField = bodyLengthField;
	}

	@Override
	public FixConverterContext marshal(byte[] src) throws ConverterException {
		FixConverterContext context = new FixConverterContext(src);
		Map<?,?> result=(Map<?, ?>) parse(context);
		if (StringUtils.isNotEmpty(this.getDto()))
			context.setDto(FixConverterContext.createDTO(this.getDto(), result));
		return context;
	}
	
	@Override
	public Object parse(FixConverterContext context) throws ConverterException {
		Map<String,Object> result = new HashMap<>();
		for (AbsFixConverter converter : this.getChildren()) {
			Object value = converter.parse(context);
			context.setBodyLength(context.get(bodyLengthField));
			
			if(StringUtils.isNotEmpty(this.getDto()) && converter instanceof IncludeTag){
				result.putAll((Map<String, Object>) value);
			}else{
				result.put(converter.getName(), value);				
			}
			
			if (logger.isTraceEnabled()) 
				logger.trace("{}:{}",converter.getName(),value);
		}
		
		return result;
	}

	
	@Override
	public String unmarshal(Object src) {
		Map<String,Object> srcMap =null;
		
		if (src instanceof Map)
			srcMap=(Map<String, Object>) src;
		else
			srcMap=ValueCopyUtil.beanToMap(src);
		
		return format(srcMap);
	}
	
	@Override
	public String format(Map<String,Object> src) throws ConverterException{
		StringBuilder sb= new StringBuilder();
		List<String> resultList= new ArrayList<>();
		for (int i=this.getChildren().size()-1;i>=0;i--){
			AbsFixConverter converter = this.getChild(i);
			String result=converter.format(src);
			if (StringUtils.equals(converter.getName(),"body")){
				FixConverterContext.nestedSet(src, getBodyLengthField(),result.length());
			}
			resultList.add(result);
		}
		for (int i=resultList.size()-1;i>=0;i--){
			sb.append(resultList.get(i));
		}
		return sb.toString();
	}
}
