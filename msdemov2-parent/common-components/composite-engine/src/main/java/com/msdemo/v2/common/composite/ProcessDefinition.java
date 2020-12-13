package com.msdemo.v2.common.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.msdemo.v2.common.composite.chain.AbsProcessInterceptor;
import com.msdemo.v2.common.composite.chain.AbsTxnInterceptor;
import com.msdemo.v2.common.composite.chain.InterceptorChain;
import com.msdemo.v2.common.composite.chain.InterceptorChain.ProcessChainBuilder;
import com.msdemo.v2.common.composite.chain.InterceptorChain.TxnChainBuilder;
import com.msdemo.v2.common.composite.flow.AbstractFlow;
import com.msdemo.v2.common.composite.flow.listeners.CompositeFlowListener;
import com.msdemo.v2.common.composite.handler.IExceptionHandler;
import com.msdemo.v2.common.composite.param.INewInstance;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.composite.spi.IResultWrapper;
import com.msdemo.v2.common.invocation.invoker.InvocationProxy;
import com.msdemo.v2.common.utils.LogUtil;
import com.msdemo.v2.common.utils.XmlUtil;
import com.msdemo.v2.resource.management.SpringContextHolder;
import com.msdemo.v2.resource.script.spi.IScriptService;

public final class ProcessDefinition {
	
	private static Logger logger =LoggerFactory.getLogger(ProcessDefinition.class);	
	
	String name;
	
	TxnType txnType=TxnType.Non;
	
	InvocationProxy invoker;
	
	IScriptService scriptService;
	
	List<MutablePair<Integer,AbsProcessInterceptor>> interceptors ;
	CompositeFlowListener defaultListener;
	
	InterceptorChain chain;
	
	IExceptionHandler exceptionHandler;
	
	List<AbstractFlow> flowList = new ArrayList<>();
	
	ParamMapping resultMapping;
	INewInstance resultObject;
	IResultWrapper<?> resultWrapper;
	
	public ProcessDefinition(String name){
		this.name=name;
	}
	
	public void apply(InterceptorChain defaultChain,IScriptService scriptService,
			CompositeFlowListener defaultFlowListener,
			boolean refreshFlag){
		if (interceptors ==null){
			this.chain=defaultChain;
		}else{
			ProcessChainBuilder pBuilder=null;
			TxnChainBuilder tBuilder=null;
			for (MutablePair<Integer,AbsProcessInterceptor> in:interceptors){
				if (in.getValue() instanceof AbsTxnInterceptor){
					tBuilder=Optional.ofNullable(tBuilder).orElseGet(()->InterceptorChain.txnBuilder())
					.add((AbsTxnInterceptor)(in.getValue()),in.getKey());
				}else
					pBuilder=Optional.ofNullable(pBuilder).orElseGet(()->InterceptorChain.builder())
					.add(in.getValue(),in.getKey());
			}
			
			this.chain=new InterceptorChain();
			if (pBuilder!=null) {
				this.chain.setProcessChain(pBuilder.build());
				this.chain.getProcessChain().merge(this.name,defaultChain.getProcessChain());
			}else{
				this.chain.setProcessChain(defaultChain.getProcessChain());
			}
			if (tBuilder!=null){
				this.chain.setTxnChain(tBuilder.build());
				this.chain.getTxnChain().merge(this.name,defaultChain.getTxnChain());
			}else{
				this.chain.setTxnChain(defaultChain.getTxnChain());
			}
		}
		this.defaultListener=defaultFlowListener;
		
		if (this.scriptService==null) this.scriptService=scriptService;
		flowList.forEach( flow ->{
				flow.verify(this,refreshFlag);
		});
	}
	public CompositionContext execute(CompositionContext context){
		String flowName=null;
		try{
			for (int i=0;i<flowList.size();i++){
				AbstractFlow flow=flowList.get(i);
				flowName=flow.getName();
				//for dynamic transaction re-execution, do NOT execute completed flow
				if (!context.containsKey(flowName)){
					flow.execute(context);
					//if transaction type changed, stop the process and return to ProcessFlowFactory
					if (context.isTxnTypeChanged()) 
						return context;
				}
			};
			//response
			if (resultMapping!=null){
				flowName=INewInstance.USAGE;
				context.setResp(resultObject.newInstance());			
				SpelExpressionParser parser = ParamMapping.parser;
				for (MutablePair<String,String> pair:resultMapping){
					Object value=parser.parseExpression(pair.getRight()).getValue(context);
					parser.parseExpression(pair.getLeft()).setValue(context,value);
				}
			}else if (resultWrapper !=null){
				context.setResp(resultWrapper.wrap(context));
			}else{
				context.setResp(context.get(flowName));//set default response as the last flow result 
			}
		}catch(Exception e){
			this.exceptionHandler.handle(flowName, context, e);
			if (context.getException()!=null) throw context.getException();
		}
		return context;
	}
	
	public CompositionContext executeObj(Object req){
		CompositionContext context = new CompositionContext(this.name,this.txnType);
		context.setInvocationProxy(this.invoker);
		context.setScriptService(scriptService);
		context.setReq(req);
		return execute(context);
	}

	public enum TxnType{		
		Global, //Global compensatable transaction 
		GlobalNoLock, // Global transaction without TXN lock
		Local, //Local Transaction, such as JDBC, JMS
		LocalNoLock, //Local Transaction, without TXN lock
		
		Dynamic, //Dynamic transaction, actual transaction type would be determined during process execution
		Prepare, //only used for JVM initialize, run dummy TXN then roll back
		Non; 	//disable transaction management, for inquire TXN
		
		public static boolean maybeDtxProcess(TxnType txnType){
			if (txnType.equals(TxnType.Global) ||
					txnType.equals(TxnType.Dynamic) ||
					txnType.equals(TxnType.GlobalNoLock) 
					){
				return true;
			}
			return false;
		}
	}
	
	public String getName(){
		return this.name;
	}
	public TxnType getTxnType(){
		return this.txnType;
	}
	
	public IScriptService getScriptService(){
		return this.scriptService;
	}
	public InvocationProxy getInvoker(){
		return this.invoker;
	}
	public String toXml(){
		StringBuilder sb =new StringBuilder();
		sb.append("<processFlow name=\"").append(name)
			.append("\" txnType=\"").append(txnType);
		if (!(InvocationProxy.class.equals(invoker.getClass())))
			sb.append("\" invoker=\"").append(SpringContextHolder.getSpringBeanClassName(invoker));		
		sb.append("\" exceptionHandler=\"").append(exceptionHandler.getClass().getName()).append("\">");
		if (interceptors!=null){
			sb.append("<interceptors>");
			for (MutablePair<Integer,AbsProcessInterceptor> pair: interceptors){
				sb.append("<interceptor order=\"").append(pair.getKey()).append("\">")
				.append(pair.getValue().getClass().getName()).append("</interceptor>");					
			}
			sb.append("</interceptors>");
		}
		sb.append("<flowList>");
		for (AbstractFlow flow:flowList){
			sb.append(flow.toXml());
		}
		sb.append("</flowList>");
		sb.append("<result");
		if (resultWrapper!=null)
			sb.append(" wrapper=\"").append(resultWrapper.getClass().getName()).append("\">");
		else{
			sb.append(">");
			if (resultObject!=null)
				sb.append("<className>").append(resultObject.getClass().getName()).append("</className>");
			if (resultMapping!=null)
				sb.append(resultMapping.toXml());
		}
		sb.append("</result></processFlow>");
		
		try {
			return XmlUtil.format(sb.toString());
		} catch (Exception e) {
			LogUtil.exceptionLog(logger, e);
			return sb.toString();
		}
	}
}
