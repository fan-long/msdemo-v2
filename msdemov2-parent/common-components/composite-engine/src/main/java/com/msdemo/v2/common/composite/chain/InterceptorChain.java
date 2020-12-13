package com.msdemo.v2.common.composite.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.CompositionFactory;
import com.msdemo.v2.common.composite.chain.AbsProcessInterceptor.InterceptorModel;

public class InterceptorChain {
	private static Logger logger =LoggerFactory.getLogger(InterceptorChain.class);
	
	private ProcessChain processChain;
	private TxnChain txnChain;
	
	public InterceptorChain(){
		this(null,null);
	}
	public InterceptorChain(ProcessChain processChain, TxnChain processTxnChain){
		this.processChain=processChain!=null?processChain:new ProcessChain();
		this.txnChain=processTxnChain!=null?processTxnChain:new TxnChain();
	}
	
	public static ProcessChainBuilder builder(){
		return new ProcessChainBuilder();
	}
	public static TxnChainBuilder txnBuilder(){
		return new TxnChainBuilder();
	}
	
	public void setProcessChain(ProcessChain processChain){
		this.processChain=processChain;
	}
	
	public ProcessChain getProcessChain(){
		return this.processChain;
	}
	
	public static class ProcessChain{
		protected List<MutablePair<Integer,AbsProcessInterceptor>> interceptors = new ArrayList<>(2);		
		protected List<AbsProcessInterceptor> orderedInterceptors;
		public CompositionContext invoke(String processName,Object requestDto){
			InterceptorModel model = buildModel(processName,requestDto,orderedInterceptors);
			if (orderedInterceptors==null || orderedInterceptors.isEmpty())
				return AbsProcessInterceptor.invoke(model);
			else{
				return this.orderedInterceptors.get(0).intercept(model);
			}
		}
		public void merge(String processName,ProcessChain otherChain){
			this.interceptors.addAll(otherChain.interceptors);
			this.orderedInterceptors=this.interceptors.stream().filter( i -> i.right!=null)
					.sorted((a,b) -> a.left.compareTo(b.left))
					.map(i -> i.right)
					.collect(Collectors.toList());
//			for (int i=0;i<this.interceptors.size()-1;i++){
//				this.interceptors.get(i).getValue().setNextHandler(
//						this.interceptors.get(i+1).getValue());
//			}
			if (logger.isDebugEnabled())
				logger.debug("{}-{} customize order: {}",
						processName,this.getClass().getSimpleName(),
					StringUtils.join(this.orderedInterceptors.stream().map( in -> 
					in.getClass().getSimpleName()).toArray(),"->"));
		}
	}
	
	public static class ProcessChainBuilder {
		ProcessChain instance = new ProcessChain();

		public ProcessChainBuilder add(AbsProcessInterceptor interceptor,int order) {
			instance.interceptors.add(new MutablePair<Integer, AbsProcessInterceptor>(order,interceptor));
			return this;
		}
		public ProcessChainBuilder add(MutablePair<Integer, AbsProcessInterceptor> interceptor) {
			instance.interceptors.add(interceptor);
			return this;
		}
		public ProcessChain build(){
			instance.orderedInterceptors=instance.interceptors.stream()
					.filter( i -> i.right!=null)
					.sorted((a,b) -> a.left.compareTo(b.left))
					.map(i -> i.right)
					.collect(Collectors.toList());			
			return instance;
		}		
	}
	
	public void setTxnChain(TxnChain txnChain){
		this.txnChain=txnChain;
	}
	public TxnChain getTxnChain(){
		return this.txnChain;
	}
	
	public static class TxnChain extends ProcessChain{
		public CompositionContext invoke(String processName,Object requestDto){
			InterceptorModel model = buildModel(processName,requestDto,orderedInterceptors);
			return invoke(model);
		}
		public CompositionContext invoke(CompositionContext context){
			InterceptorModel model = buildModel(context.getProcessName(),context.getReq(),orderedInterceptors);
			model.context=context;
			return invoke(model);
		}
		private CompositionContext invoke(InterceptorModel model){
			if (orderedInterceptors==null || orderedInterceptors.isEmpty())
				return AbsTxnInterceptor.txnInvoke(model);
			else{				
				return this.orderedInterceptors.get(0).intercept(model);
			}
		}
	}
	
	public static class TxnChainBuilder {
		TxnChain instance = new TxnChain();

		public TxnChainBuilder add(AbsTxnInterceptor interceptor,int order) {
			instance.interceptors.add(new MutablePair<Integer, AbsProcessInterceptor>(order,interceptor));
			return this;
		}
		public TxnChainBuilder add(MutablePair<Integer, AbsProcessInterceptor> interceptor) {
			instance.interceptors.add(interceptor);
			return this;
		}
		public TxnChain build(){
			instance.orderedInterceptors=instance.interceptors.stream()
					.filter( i -> i.right!=null)
					.sorted((a,b) -> a.left.compareTo(b.left))
					.map(i -> i.right)
					.collect(Collectors.toList());	
			return instance;
		}	
	}
	
	private static InterceptorModel buildModel(String processName,Object requestDto,
			List<AbsProcessInterceptor> interceptors){
		InterceptorModel model = new InterceptorModel();
		model.definition=CompositionFactory.get(processName);
		model.requestDto=requestDto;
		model.txnContainer=CompositionFactory.getTxnContainer();
		model.interceptors=interceptors;
		return model;
	}
}