package com.msdemo.v2.common.cache.core;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 可用于数据查询服务中的查询方法，方法名前缀为selectBy
 * 并且查询服务须实现 ICacheParamTable接口
 * @author LONGFAN
 *
 */
@Target({METHOD})
@Retention(RUNTIME)
@Inherited
@Documented
public @interface CachedQuery {
	
	@AliasFor("field")
	String[] value();

	//是否返回clone后的对象
	boolean cloned() default true;
	
	//参数是否自定义DTO对象,如果是DTO，必须指定索引的field
	boolean dto() default false;
	
	//是否穿透缓存直接访问native method
	boolean pernetrate() default false;
}
