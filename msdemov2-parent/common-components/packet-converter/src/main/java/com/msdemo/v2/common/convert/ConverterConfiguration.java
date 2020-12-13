package com.msdemo.v2.common.convert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.convert.core.ConverterFactory;
import com.msdemo.v2.common.convert.definition.DefinitionParserFactory;
import com.msdemo.v2.common.convert.definition.fix.FixDefinitionHolder;
import com.msdemo.v2.common.convert.definition.flat.FlatDefinitionHolder;

@Configuration
public class ConverterConfiguration {

	public static final String CONFIG_ROOT=CommonConstants.CONFIG_ROOT_PREFIX+".converter";
	
	@Autowired
	DefinitionParserFactory definitionFactory;
	
	@Bean
	ConverterFactory fixConverterFactory(){
		ConverterFactory bean = new ConverterFactory();
		bean.setDefinitionFactory(definitionFactory);
		bean.setDefinitionType(FixDefinitionHolder.DEFINITION_TYPE);
		return bean;
	}
	
	@Bean
	ConverterFactory flatConverterFactory(){
		ConverterFactory bean = new ConverterFactory();
		bean.setDefinitionFactory(definitionFactory);
		bean.setDefinitionType(FlatDefinitionHolder.DEFINITION_TYPE);
		return bean;
	}
}
