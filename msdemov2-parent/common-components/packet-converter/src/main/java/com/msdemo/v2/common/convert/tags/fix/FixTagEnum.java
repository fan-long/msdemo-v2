package com.msdemo.v2.common.convert.tags.fix;

import com.msdemo.v2.common.convert.definition.fix.AbsFixConverter;

public enum FixTagEnum {

	message(MessageTag.class),
	segment(SegmentTag.class),
	include(IncludeTag.class),
	loop(LoopTag.class),
	
	fixString(FixStringTag.class),
	fixInteger(FixInteger.class),
	fixTimeStamp(FixTimeStamp.class),
	fixBigDecimal(FixBigDecimal.class);
	
	private Class<? extends AbsFixConverter> tagClass;
	
	FixTagEnum(Class<? extends AbsFixConverter> tagClass){
		this.tagClass=tagClass;
	}

	public Class<? extends AbsFixConverter> getTagClass() {
		return tagClass;
	}

	public static Class<? extends AbsFixConverter> getClassByTagName(String tagName){
		return FixTagEnum.valueOf(tagName).tagClass;
	}
}
