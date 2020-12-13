package com.msdemo.v2.common.convert.definition.flat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.tags.flat.AbsFlatConverter;
import com.msdemo.v2.common.utils.ValueCopyUtil;

public class FlatRootConverter extends AbsFlatConverter<Object> implements IRootConverter<FlatConverterContext>{


	List<AbsFlatConverter<?> > cachedRequestConverter =new ArrayList<>();
	List<AbsFlatConverter<?> > cachedResponseConverter =new ArrayList<>();

	private String delimiter;
	private Class<?> requestClass;
	
	public void addRequestConverter(AbsFlatConverter<?> converter){
		cachedRequestConverter.add(converter);
	}
	public void addResponseConverter(AbsFlatConverter<?> converter){
		cachedResponseConverter.add(converter);
	}
	@Override
	public FlatConverterContext marshal(byte[] src) {
		String[] fields=StringUtils.split(new String(src),delimiter);
		FlatConverterContext context=new FlatConverterContext(fields);
		Map<String,Object> resultMap=new HashMap<>();
		for (int i=0;i<cachedRequestConverter.size();i++){
			resultMap.put(cachedRequestConverter.get(i).getName(), 
					cachedRequestConverter.get(i).parse(context));
		}
		Object resultDto;
		try {
			resultDto = requestClass.newInstance();
		} catch (Exception e) {
			throw new ConverterException(String.format(ConverterException.DTO_INSTANCE,
					requestClass.getName(),e.getMessage()));
		}
		ValueCopyUtil.mapToBean(resultMap, resultDto);
		context.setDto(resultDto);
		return context;
	}

	@Override
	public String unmarshal(Object dto) {
		Map<String,Object> resultMap=ValueCopyUtil.beanToMap(dto);
		StringBuilder result =new StringBuilder();
		for (int i=0;i<cachedResponseConverter.size()-1;i++){
			result.append(cachedResponseConverter.get(i).format(
					resultMap.get(cachedResponseConverter.get(i).getName()))).append(this.delimiter);
		}
		result.append(cachedResponseConverter.get(cachedResponseConverter.size()-1).format(
				resultMap.get(cachedResponseConverter.get(cachedResponseConverter.size()-1).getName())));
		return result.toString();
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	public void setRequestClass(Class<?> requestClass) {
		this.requestClass = requestClass;
	}
	@Override
	public Object parse(FlatConverterContext context) {
		Map<String,Object> resultMap=new HashMap<>();
		for (int i=0;i<cachedRequestConverter.size();i++){
			resultMap.put(cachedRequestConverter.get(i).getName(), 
					cachedRequestConverter.get(i).parse(context));
		}
		Object resultDto;
		try {
			resultDto = requestClass.newInstance();
		} catch (Exception e) {
			throw new ConverterException(String.format(ConverterException.DTO_INSTANCE,
					requestClass.getName(),e.getMessage()));
		}
		ValueCopyUtil.mapToBean(resultMap, resultDto);
		return resultDto;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String format(Object fieldValue) {
		Map<String,Object> childMap= (HashMap<String,Object>)fieldValue;
		StringBuilder result= new StringBuilder();
		for (int i=0;i<cachedResponseConverter.size()-1;i++){
			result.append(cachedResponseConverter.get(i)
					.format(childMap.get(cachedResponseConverter.get(i).getName())))
			.append(this.delimiter);
		}
		result.append(cachedResponseConverter.get(cachedResponseConverter.size()-1)
				.format(childMap.get(cachedResponseConverter.get(cachedResponseConverter.size()-1).getName())));
		return result.toString();
	}
	

}
