package com.msdemo.v2.common.convert.tags.flat;

import com.msdemo.v2.common.convert.core.ConverterException;

public enum FlatTagEnum {

	FlatString(FlatStringTag.class,"string"),
//	CsvInteger(CsvIntegerTag.class,"integer"),
	FlatTimeStamp(FlatTimestampTag.class,"timestamp"),
	FlatBigDecimal(FlatBigDecimalTag.class,"bigDecimal"),
	FlatDto(FlatDtoTag.class,"dto");
	
	private Class<? extends AbsFlatConverter<?>> tagClass;
	private String tagName;
	
	FlatTagEnum(Class<? extends AbsFlatConverter<?>> tagClass,String tagName){
		this.tagClass=tagClass;
		this.tagName=tagName;
	}

	public Class<? extends AbsFlatConverter<?>> getTagClass() {
		return tagClass;
	}

	public static Class<? extends AbsFlatConverter<?>> getClassByTagName(String tagName){
		return FlatTagEnum.valueOf(tagName).tagClass;
	}
	public static AbsFlatConverter<?> getInstanceByTagName(String tagName){
		try {
			return FlatTagEnum.valueOf(tagName).tagClass.newInstance();
		} catch (Exception e) {
			throw new ConverterException(String.format(ConverterException.DTO_INSTANCE,
					FlatTagEnum.valueOf(tagName).tagClass.getName(),e.getMessage()));
		}
	}
	public static FlatTagEnum byName(String tagName){
		for (FlatTagEnum tag: FlatTagEnum.values()){
			if (tag.tagName.equals(tagName))
				return tag;
		}
		throw new IllegalArgumentException(tagName +" is invalid tag name");
	}
}
