package com.msdemo.v2.common.convert.definition.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.convert.core.ConverterException;

public abstract class AbsFixContainer extends AbsFixConverter  {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	private String dto;

	//THREAD-SAFE: only used on the first XML parsing/cache process, DO NOT update it again.
	private List<AbsFixConverter> container = new ArrayList<>();

	public void addChild(AbsFixConverter tag) {
		this.container.add(tag);
	}

	public List<AbsFixConverter> getChildren() {
		return this.container;
	}

	public AbsFixConverter getChild(int i) {
		return this.container.get(i);
	}

	public void addChild(int i, AbsFixConverter tag) {
		this.container.add(i, tag);
	}

	public void removeChild(int i) {
		this.container.remove(i);
	}

	public void removechild(AbsFixConverter tag) {
		this.container.remove(tag);
	}

	@Override
	public Object parse(FixConverterContext context) throws ConverterException {
		Map<String, Object> dtoResult = new HashMap<>();
		Map<String,Object> mapResult = new HashMap<>();
		for (AbsFixConverter converter : this.getChildren()) {
			String childName=converter.getName();
			Object value = converter.parse(context);
			dtoResult.put(childName, value);	
			if (converter instanceof AbsFixContainer){
				mapResult.put(childName, context.get(childName));
				context.remove(childName);
			}else{
				mapResult.put(childName, value);
			}
			if (logger.isTraceEnabled()) 
				logger.trace("{}:{}",converter.getName(),value);
		}
		if (StringUtils.isNotEmpty(this.getName())){
			context.set(this.getName(), mapResult);
			if (StringUtils.isNotEmpty(this.getDto())) {			
				return FixConverterContext.createDTO(this.getDto(), dtoResult);
			}
		}		
		return dtoResult;
	}

	@Override
	abstract public String format(Map<String,Object> src) throws ConverterException;
	

	public String getDto() {
		return dto;
	}

	public void setDto(String dto) {
		this.dto = dto;
	}


}
