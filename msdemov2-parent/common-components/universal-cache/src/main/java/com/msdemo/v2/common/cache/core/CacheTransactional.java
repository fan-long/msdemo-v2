package com.msdemo.v2.common.cache.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(isolation= Isolation.READ_COMMITTED,
	rollbackFor={Exception.class})
public @interface CacheTransactional {

	@AliasFor(annotation = Transactional.class, attribute = "value")
	String value() default "";

	@AliasFor(annotation = Transactional.class, attribute = "timeout")
	int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;
	
	@AliasFor(annotation = Transactional.class, attribute = "propagation")
	Propagation propagation() default Propagation.REQUIRED;
}
