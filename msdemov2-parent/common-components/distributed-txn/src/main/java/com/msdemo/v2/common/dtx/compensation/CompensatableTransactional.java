package com.msdemo.v2.common.dtx.compensation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.msdemo.v2.common.dtx.core.IDtxCoordinator;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CompensatableTransactional {
	
	public enum CompensationType{
		AT, //auto-compensate by undo sql
		MT, //manually-compensate by code
	}
	public enum Mode {
		Sync, // sync insert(merge) journal, async-compensation
		Async  // async insert journal and update transaction status, async-compensation 
	}
	CompensationType compensationType() default CompensationType.MT;
	Mode mode() default Mode.Sync;
	
	Class<? extends ICompensatable> compensatorClass() default ICompensatable.Empty.class;
	String compensator() default "";
	
	boolean entry() default false;
	
	//EL expression for distribution lock
	String[] lock() default {};
	
	int timeout() default IDtxCoordinator.DEFAULT_TIME_OUT;


}
