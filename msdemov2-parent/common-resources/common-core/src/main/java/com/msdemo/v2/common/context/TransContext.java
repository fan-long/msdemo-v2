package com.msdemo.v2.common.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.msdemo.v2.common.ManagedThreadLocal;
import com.msdemo.v2.common.exception.TransException;
import com.msdemo.v2.common.utils.DateTimeUtil;

public class TransContext {

	public static class Context{
		//可传递，下游不可更改
		public Common common = new Common();
		//可传递，下游可增加
		public Exchange exchange = new Exchange();
		//不可传递
		public Map<String,Object> local = new HashMap<>();
		private AtomicInteger sequence = new AtomicInteger(0);
		public Integer nextSequence(){
			Integer next= sequence.addAndGet(1);
			common.setSequenceId(next);
			return next;
		}
	}
	public static class Common{
		private String traceId;
		private Integer sequenceId=0;
		private String transDate;
		private String acctDate;
		private String channel;
		private Txn txn = new Txn();
		//depends on NTP to ensure millisecond level clock gap during cross-node interactive
		private long deadline; 
		private boolean nightlyMode;
		private String debug;
		
		public static class Txn{
			//to identify the transaction context with Txn-Agent
			private String dtxId;	
			private String lock="N";
			
			public String getLock(){
				return lock;
			}
			public void setLock(String lock) {
				this.lock = lock;
			}
			public String getDtxId() {
				return dtxId;
			}
			public void setDtxId(String dtxId) {
				this.dtxId = dtxId;
			}			
		}
		
		public long getDeadline() {
			return deadline;
		}
		public void setDeadline(long deadline) {
			this.deadline = deadline;
		}
		
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
		public String getChannel() {
			return channel;
		}
		public void setChannel(String channel) {
			this.channel = channel;
		}
		public void setSequenceId(Integer sequenceId){
			this.sequenceId=sequenceId;
		}
		public Integer getSequenceId() {
			return sequenceId;
		}
		public String getDebug() {
			return debug;
		}
		public void setDebug(String debug) {
			this.debug = debug;
		}
		public Txn getTxn() {
			return txn;
		}
		public void setTxn(Txn txn) {
			this.txn = txn;
		}	
	}
	
	
	public static class Exchange{
		public OneWay oneway= new OneWay();
		public TwoWay twoway = new TwoWay();
		
		@JsonIgnore
		public void merge(Exchange exchange){
			this.oneway.add(exchange.oneway);
			this.twoway.append(exchange.twoway);
		}
		//pass to downstream and expand new key
		public static class TwoWay{
			private HashMap<String,String> paramVersion= new HashMap<>(4,1);
			private Set<String> businessEvents = new HashSet<>(2);
			private Set<String> locks= new HashSet<>(1);

			public HashMap<String, String> getParamVersion() {
				return paramVersion;
			}
			public Set<String> getBusinessEvents(){
				return businessEvents;
			}
			public Set<String> getLocks() {
				return locks;
			}
			@JsonIgnore
			void append(TwoWay twoway){
				for (String key: twoway.paramVersion.keySet()){
					this.paramVersion.putIfAbsent(key, twoway.paramVersion.get(key));
				}
				businessEvents.addAll(twoway.businessEvents);
				locks.addAll(twoway.locks);
			}
		}
		//get from downstream and expand new item
		public static class OneWay{
			private List<String> journalList = new ArrayList<>(2);
			private List<String> smsList = new ArrayList<>(1);
			private List<String> unLockList= new ArrayList<>(2);
			public List<String> getJournalList() {
				return journalList;
			}	
			public List<String> getSmsList() {
				return smsList;
			}
					
			@JsonIgnore
			void add(OneWay oneway){
				if (oneway.journalList.size()>0)
					journalList.addAll(oneway.journalList);
				if (oneway.smsList.size()>0)
					smsList.addAll(oneway.smsList);
				if (oneway.unLockList.size()>0)
					unLockList.addAll(oneway.unLockList);
			}
			public List<String> getUnLockList() {
				return unLockList;
			}
			public void setUnLockList(List<String> unLockList) {
				this.unLockList = unLockList;
			}
		}
	}
	
	static final ManagedThreadLocal<Context> ContextHolder= new ManagedThreadLocal<>(
			TransContext.class.getSimpleName(), Context.class,true);

	public static Context get(){
		return ContextHolder.get();
	} 
	//used for sharing context between parent and child thread
	public static void replace(Context context){
		ContextHolder.set(context);
	}
	public static boolean isDistributedTransaction(){
		return get().common.txn.getDtxId()!=null;
	}

	public static void verifyDeadline(){
		if (Instant.now().toEpochMilli()>get().common.getDeadline()){
			if (isDistributedTransaction())
				throw new TransException(TransException.RESPONSE_CODE_TIMEOUT,
					"distributed transaction# "+get().common.txn.dtxId+" timed out, deadline is "+
					DateTimeUtil.timestamp(TransContext.get().common.getDeadline()));
			else
				throw new TransException(TransException.RESPONSE_CODE_TIMEOUT,
						"transaction timed out, deadline is "+
						DateTimeUtil.timestamp(TransContext.get().common.getDeadline()));
		}	
	}
}
