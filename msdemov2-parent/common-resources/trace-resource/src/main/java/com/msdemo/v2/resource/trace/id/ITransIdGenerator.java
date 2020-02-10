package com.msdemo.v2.resource.trace.id;

public interface ITransIdGenerator {
	static final String TRACE_ID="traceId";
	String nextId();
}
