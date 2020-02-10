package com.msdemo.v2.common.util;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueCopy {

	boolean ignored() default false;
	boolean nested() default false;
	
}
