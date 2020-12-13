package com.msdemo.v2.common.convert.definition.flat;

import com.msdemo.v2.common.convert.core.IConverterContext;

public class FlatConverterContext implements IConverterContext {

	private int index=0;
	private String[] fields;
	private Object resultDto;
	
	public FlatConverterContext(String[] fields){
		this.fields=fields;
	}
	@Override
	public void setDto(Object resultDto) {
		this.resultDto=resultDto;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getDto() {
		return (T) this.resultDto;
	}	
	public String[] getFields() {
		return fields;
	}
	public String getAndForward(){
		return fields[index++];
	}
}
