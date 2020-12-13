package com.msdemo.v2.common.composite.flow;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.flow.listeners.CompositeFlowListener;
import com.msdemo.v2.common.composite.flow.listeners.IFlowListener;
import com.msdemo.v2.common.context.TransContext;

@SuppressWarnings({"rawtypes","unchecked"})
public abstract class AbstractFlow {

	String name;
	String mergeName;
	CompositeFlowListener listener;
	boolean enableJournal;
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	abstract protected void doExecute(CompositionContext context) throws Exception;
	
	public final void execute(CompositionContext context) throws Exception{
		if (context.getDefaultListener()!=null){
			context.getDefaultListener().beforeFlow(this, context);
		}
		if (this.listener!=null){
			listener.beforeFlow(this, context);
		}
		TransContext.get().nextSequence();
		doExecute(context);
		if (this.listener!=null){
			listener.afterFlow(this, context);
		}
		if (context.getDefaultListener()!=null){
			context.getDefaultListener().afterFlow(this, context);
		}
	}
	protected void beforeInvoke(CompositionContext context,Object[] args){
		if (context.getDefaultListener()!=null){
			context.getDefaultListener().beforeInvoke(this, context,args);
		}
		if (this.listener!=null){
			listener.beforeInvoke(this, context,args);
		}
	}
	abstract public void verify(ProcessDefinition definition,boolean refreshFlag);
	
	public StringBuilder toXml(){
		StringBuilder sb = new StringBuilder();
		sb.append("<name");
		if (StringUtils.isNotEmpty(mergeName))
			sb.append(" merge=\"").append(mergeName).append("\"");
		sb.append(">").append(name).append("</name>");
		if (enableJournal)
			sb.append("<journal>true</journal>");
		if (this.listener!=null){
			sb.append("<listeners>");
			for (IFlowListener l: this.listener.getDelegates()){
				sb.append("<listener>").append(l.getClass().getSimpleName()).append("</listener>");
			}
			sb.append("</listeners>");
		}
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
		public P journal(boolean enableJournal){
			t.enableJournal=enableJournal;
			return (P)this;
		}
		public P addListener(IFlowListener listener){
			if (t.listener==null) t.listener=new CompositeFlowListener();
			t.listener.addListener(listener);
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
	public boolean isJournalEnabled(){
		return enableJournal;
	}
}
