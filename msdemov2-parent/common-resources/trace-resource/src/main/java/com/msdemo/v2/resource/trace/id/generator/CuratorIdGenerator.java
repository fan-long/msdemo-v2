package com.msdemo.v2.resource.trace.id.generator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;

import com.msdemo.v2.resource.trace.id.ITransIdGenerator;

public class CuratorIdGenerator implements ITransIdGenerator{	

	private String transIdNode;
	
	private DistributedAtomicLong distAtomicLong;
	
	public CuratorIdGenerator(CuratorFramework client,RetryPolicy retryPolicy,String node){
		this.transIdNode=node;
		distAtomicLong = new DistributedAtomicLong(client, transIdNode, retryPolicy);
	}
	
	public String nextId(){
		AtomicValue<Long> sequence;
		try {
			sequence = distAtomicLong.increment();			
		} catch (Exception e) {
        	throw new RuntimeException("failed: " +e.getMessage());
		}
		if (sequence.succeeded()) 
        	return String.valueOf(sequence.postValue());
		else
			throw new RuntimeException("sequence.failed");
	}
}


