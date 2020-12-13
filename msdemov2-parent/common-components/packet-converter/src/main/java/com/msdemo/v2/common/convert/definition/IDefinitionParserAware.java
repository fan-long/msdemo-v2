package com.msdemo.v2.common.convert.definition;

import com.msdemo.v2.common.convert.core.IConverterContext;

public interface IDefinitionParserAware<T extends IConverterContext> {

	void setDefinitionParser(IDefinitionHolder<T> parser);
}
