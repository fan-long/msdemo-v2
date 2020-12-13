package com.msdemo.v2.common.cache.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@SuppressWarnings("unchecked")
public abstract class AbstractCachedObject<T> implements Cloneable {

	/** shallow clone
	 * to prevent try-catch within loop, throws exception without catch */
	@Override
	public T clone() throws CloneNotSupportedException {
		return (T) super.clone();
	}

	/**
	 * deep clone
	 * or SerializationUtils.clone()
	 * or json serialization
	 * @return
	 * @throws Exception
	 */
	public T deepClone() throws Exception {
		// 将对象写到流里
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream oo = new ObjectOutputStream(bo);
		oo.writeObject(this);
		// 从流里读出来
		ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
		ObjectInputStream oi = new ObjectInputStream(bi);
		return (T) (oi.readObject());

	}
}
