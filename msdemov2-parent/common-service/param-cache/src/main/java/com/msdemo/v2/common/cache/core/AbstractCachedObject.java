package com.msdemo.v2.common.cache.core;


public abstract class AbstractCachedObject<T> implements Cloneable {

	@SuppressWarnings("unchecked")
	@Override
    public T clone() throws RuntimeException {
        try {
			return (T) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("clone NOT supported");
		}
    }
}
