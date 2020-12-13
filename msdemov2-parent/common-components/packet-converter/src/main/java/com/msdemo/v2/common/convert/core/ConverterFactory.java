package com.msdemo.v2.common.convert.core;

import org.springframework.lang.NonNull;

import com.msdemo.v2.common.convert.definition.DefinitionParserFactory;
import com.msdemo.v2.common.convert.definition.IDefinitionHolder;

public class ConverterFactory {
	
	@NonNull
	private String definitionType;

	DefinitionParserFactory definitionFactory;
	
	IDefinitionHolder<?> definitionHolder;
	
	public IRootConverter<?> getInstance(String definitionFileName) {
		return definitionHolder.fromFile(definitionFileName);
	}
	
	public void setDefinitionType(String type){
		this.definitionType = type;
		definitionHolder=definitionFactory.getInstance(definitionType);
	}

	public void setDefinitionFactory(DefinitionParserFactory definitionFactory) {
		this.definitionFactory = definitionFactory;
	}

	
}
