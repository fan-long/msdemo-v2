package com.msdemo.v2.common.param.table.entity;

import com.msdemo.v2.common.cache.core.AbstractCachedObject;
import com.msdemo.v2.common.param.consistent.IDateVersionAwareParam;

public class SystemProperty extends AbstractCachedObject<SystemProperty>
	implements IDateVersionAwareParam {

	private String code;

	private String value;

	private String version;

	private String effectiveDate;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(String effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	@Override
	public String getDate() {
		return effectiveDate;
	}

	@Override
	public String getCombinedLogicKey() {
		return code;
	}
}