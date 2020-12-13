package com.msdemo.v2.common.composite.handler;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.param.ParamMapping;

public interface IExceptionHandler {
	default void setMapping(ParamMapping mapping){}
	default void setResultType(Class<?> objectClass){}
	void handle(String flowName,CompositionContext context,Exception e);
}
