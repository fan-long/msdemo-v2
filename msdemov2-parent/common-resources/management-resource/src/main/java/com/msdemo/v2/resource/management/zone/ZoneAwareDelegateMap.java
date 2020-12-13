package com.msdemo.v2.resource.management.zone;

import java.util.HashMap;

public class ZoneAwareDelegateMap<I extends java.lang.Number,V> extends HashMap<I, V> {

	private static final long serialVersionUID = -6730979113275718910L;

	public ZoneAwareDelegateMap(){
		super(2,1);
	}
	public V delegate(){
		return this.get(ZoneAwareResourceHolder.getBoundZoneId());
	}
}
