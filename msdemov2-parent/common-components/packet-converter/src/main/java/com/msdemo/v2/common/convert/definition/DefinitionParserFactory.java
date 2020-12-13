package com.msdemo.v2.common.convert.definition;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefinitionParserFactory  {
	
	private Map<String, IDefinitionHolder<?>> parserMap= new HashMap<>();
	
	public DefinitionParserFactory(@Autowired IDefinitionHolder<?>[] parsers){
		for (IDefinitionHolder<?> parser: parsers){
			parserMap.put(parser.name(), parser);			
		}
	}

	public IDefinitionHolder<?> getInstance(String definitionType){
		return parserMap.get(definitionType);
	}

	
	
}
