package com.msdemo.v2.common.compose.param;

import com.msdemo.v2.common.verification.IVerificationParam;

public class ContextAwareSpELParam implements IVerificationParam {

	private String param;

	@Override
	public Object getValue(Object paramContext) {
		if (param==null || paramContext==null)
			throw new RuntimeException("context and parameter required");
		return ParamMapping.parser.parseExpression(param).getValue(paramContext);
	}

	@Override
	public void setParam(String param) {
		this.param=param;		
	}

}
