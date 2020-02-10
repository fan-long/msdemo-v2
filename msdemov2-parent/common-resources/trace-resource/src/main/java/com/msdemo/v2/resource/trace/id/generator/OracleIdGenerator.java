package com.msdemo.v2.resource.trace.id.generator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.msdemo.v2.resource.trace.id.ITransIdGenerator;

public class OracleIdGenerator implements ITransIdGenerator {

	private static final String SQL="select "+TRACE_ID+".nextval from dual";
	@Autowired
	JdbcTemplate jdbc;
	
	@Override
	public String nextId() {
		return String.valueOf(jdbc.queryForObject(SQL, Long.class));
	}

}
