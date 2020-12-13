package com.msdemo.v2.common.cache.core;

import org.aspectj.lang.ProceedingJoinPoint;

public interface ICacheRedoObserver {

	void redo(ProceedingJoinPoint pjd);
}
