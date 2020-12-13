package com.msdemo.v2.common.core;

public interface INameAwareBean {
	
	default String name(){ return this.getClass().getSimpleName();}

}
