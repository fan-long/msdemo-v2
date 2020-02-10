package com.msdemo.v2.common.context;

import java.util.HashMap;

import com.msdemo.v2.common.ManagedThreadLocal;

public class TransContext {

	public static class Context{
		private String traceId;
		private String transDate;
		private String acctDate;
		private boolean nightlyMode;
		
		private HashMap<String,String> paramVersion= new HashMap<>();
		public String getTransDate() {
			return transDate;
		}
		public void setTransDate(String transDate) {
			this.transDate = transDate;
		}
		public String getAcctDate() {
			return acctDate;
		}
		public void setAcctDate(String acctDate) {
			this.acctDate = acctDate;
		}
		public HashMap<String, String> getParamVersion() {
			return paramVersion;
		}
		public void setParamVersion(HashMap<String, String> paramVersion) {
			this.paramVersion = paramVersion;
		}
		public String getTraceId() {
			return traceId;
		}
		public void setTraceId(String traceId) {
			this.traceId = traceId;
		}
		public boolean isNightlyMode() {
			return nightlyMode;
		}
		public void setNightlyMode(boolean nightlyMode) {
			this.nightlyMode = nightlyMode;
		}		
	}
	
	static final ManagedThreadLocal<Context> ContextHolder= new ManagedThreadLocal<>(
			TransContext.class.getSimpleName(), Context.class,true);

	public static Context get(){
		return ContextHolder.get();
	} 
}
