package com.msdemo.v2.common.convert.definition;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IConverterContext;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.core.INameAwareBean;

public interface IDefinitionHolder<T extends IConverterContext> extends INameAwareBean{

	IRootConverter<T> fromFile(String definitionFileName) throws ConverterException;
	
}
