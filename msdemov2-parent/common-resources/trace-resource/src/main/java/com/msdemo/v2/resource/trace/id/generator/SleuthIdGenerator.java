package com.msdemo.v2.resource.trace.id.generator;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;

import com.msdemo.v2.resource.trace.id.ITransIdGenerator;

public class SleuthIdGenerator implements ITransIdGenerator {

	@Override
	public String nextId() {
		return Optional.ofNullable(MDC.get(TRACE_ID).toString()).orElseGet(
				()->{ String traceId= UUID.randomUUID().toString();
				MDC.put(TRACE_ID, traceId);
				return traceId;});
	}

}
