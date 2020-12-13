package com.msdemo.v2.common.convert.definition.fix;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IConverterContext;
import com.msdemo.v2.common.utils.ValueCopyUtil;

@SuppressWarnings("unchecked")
public class FixConverterContext implements IConverterContext{
	private static final String SPLITTER=".";
	private int position =0;
	private byte[] src;
	private int bodyLength;
	
	private  Object targetObject;
	private Map<String, Object> fieldMap = new LinkedHashMap<>();
	
	public FixConverterContext(byte[] src){
		this.src=src;
	}
	@Override
	public void setDto(Object o){
		this.targetObject=o;
	}
	@Override
	public <T> T getDto(){
		return (T)targetObject;
	}
	
	public <T> T get(String key){
		return nestedGet(fieldMap,key);
	}
	
	public void set(String key, Object value){
		fieldMap.put(key, value);
	}
	
	public void remove(String key){
		fieldMap.remove(key);
	}
	public String getAndForward(int length){
		int lastPostion= position;
		position += length;
		byte[] valueBytes= new byte[length];
		try {
			System.arraycopy(src, lastPostion, valueBytes, 0, length);
		} catch (Exception e) {
			throw new ConverterException(String.format(ConverterException.PARSER_FAILED, "body.length",e.getClass().getSimpleName()));
		}
		return new String(valueBytes,StandardCharsets.UTF_8);
	}

	public int getBodyLength() {
		return bodyLength;
	}

	public void setBodyLength(int bodyLength) {
		this.bodyLength = bodyLength;
	}
	
	public static Object createDTO(String className, Map<?,?> map){
		try {
			Object dtoInstance= Class.forName(className).newInstance();
			ValueCopyUtil.mapToBean((Map<String, ?>) map, dtoInstance);
			return dtoInstance;
		} catch (Exception e) {
			throw new ConverterException(
					String.format(ConverterException.DTO_INSTANCE,className,e.getMessage()));
		}
	}

	public static <T> T nestedGet(Map<String,?> map,String key){
		if (key.indexOf(SPLITTER)==-1) 
			return (T) map.get(key);
		else{
		 String[] levels=StringUtils.split(key,SPLITTER);
		 Map<String,?> result =map;
		 int i=0;
		 while (i<levels.length-1){
			 result = (Map<String,Object>)result.get(levels[i++]);
		 }
		 return (T) result.get(levels[i]);
		}
	}

	public static void nestedSet(Map<String, Object> map, String key, Object value) {

		if (key.indexOf(SPLITTER) == -1)
			map.put(key, value);
		else {
			Map<String, Object> result = map;
			String[] levels = StringUtils.split(key, SPLITTER);
			int i = 0;
			while (i < levels.length - 1) {
				result = (Map<String, Object>) result.get(levels[i++]);
			}
			result.put(levels[i], value);
		}
	}
}
