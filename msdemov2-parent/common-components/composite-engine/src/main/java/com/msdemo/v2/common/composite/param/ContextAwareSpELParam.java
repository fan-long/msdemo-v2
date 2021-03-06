package com.msdemo.v2.common.composite.param;

import com.msdemo.v2.common.verification.chain.IVerificationParam;

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

	@Override
	public String getParam() {
		return this.param;
	}

}
