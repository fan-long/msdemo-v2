package com.msdemo.v2.common.cache.core;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 仅用于MyBatis SQLMapper接口中的查询方法，方法名前缀为selectBy
 * 并且SQLMapper 必须实现 ICacheParamTable接口
 * @author LONGFAN
 *
 */
@Target({METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Documented
public @interface CachedQuery {
	
	@AliasFor("field")
	String[] value();

	boolean cloned() default true;
}
