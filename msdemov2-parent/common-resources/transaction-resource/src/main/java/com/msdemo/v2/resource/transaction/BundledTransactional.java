package com.msdemo.v2.resource.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;

/**
 * 1.5PC transaction 
 * @author LONGFAN
 * @see BundledTransactionAspect.java
 * @see org.springframework.data.transaction.ChainedTransactionManager
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BundledTransactional {

	@AliasFor("priorities")
	Class<? extends PlatformTransactionManager>[] value() default {DataSourceTransactionManager.class};
	
    Propagation propagation() default Propagation.REQUIRED;
    
    int timeout() default 10;

}
