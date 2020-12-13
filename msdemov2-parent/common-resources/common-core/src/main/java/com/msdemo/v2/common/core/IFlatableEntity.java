package com.msdemo.v2.common.core;

import java.util.Map;

public interface IFlatableEntity {
	static final String DEFAULT_DELIMITER=",";
	default String toFlatString(String delimiter){ return "";}
	default String mapToString(Map<String,Object> rowMap,String delimter){return "";}
}
