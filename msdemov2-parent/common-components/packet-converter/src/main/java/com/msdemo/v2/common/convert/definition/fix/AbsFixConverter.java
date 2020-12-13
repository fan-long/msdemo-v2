package com.msdemo.v2.common.convert.definition.fix;

import com.msdemo.v2.common.convert.core.IConverter;

public abstract class AbsFixConverter implements IConverter<FixConverterContext>{

	private String name;
	private AbsFixConverter parent;
	private String content;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public AbsFixConverter getParent() {
		return parent;
	}
	public void setParent(AbsFixConverter parent) {
		this.parent = parent;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
}
