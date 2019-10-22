package com.msdemo.v2.common.compose.handler;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.param.ParamMapping;

public interface IExceptionHandler {
	default void setMapping(ParamMapping mapping){}
	default void setResultType(Class<?> objectClass){}
	void handle(String flowName,ProcessFlowContext context,Exception e);
}
