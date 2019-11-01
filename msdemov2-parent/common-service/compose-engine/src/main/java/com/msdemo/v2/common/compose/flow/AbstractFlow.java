package com.msdemo.v2.common.compose.flow;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.compose.ProcessFlowContext;

@SuppressWarnings({"rawtypes","unchecked"})
public abstract class AbstractFlow {

	String name;
	String mergeName;
	
	abstract public void execute(ProcessFlowContext context) throws Exception;
	
	abstract public void verify();
	
	public StringBuilder toXml(){
		StringBuilder sb = new StringBuilder();
		sb.append("<name");
		if (StringUtils.isNotEmpty(mergeName))
			sb.append(" merge=\"").append(mergeName).append("\"");
		sb.append(">").append(name).append("</name>");
		return sb;
	}
	
	public static abstract class AbstractFlowBuilder<T extends AbstractFlow, P extends AbstractFlowBuilder>{
		public AbstractFlowBuilder(){
			this.t=init();
		}
		private T t ;
		abstract T init();
		
		public T getFlow(){
			return t;
		}
		
		public P name(String name){
			t.name=name;
			return (P)this;
		}
		public P mergeName(String mergeName){
			t.mergeName=mergeName;
			return (P)this;
		}
		public T build(){
			return t;
		}
	}

	static class FlowFactory<T extends AbstractFlowBuilder>{
		public T get(Class<T> clz) {
			try {
				return clz.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}	
	
	public String getMergeName() {
		return mergeName;
	}

	
	public String getName() {
		return name;
	}
}
