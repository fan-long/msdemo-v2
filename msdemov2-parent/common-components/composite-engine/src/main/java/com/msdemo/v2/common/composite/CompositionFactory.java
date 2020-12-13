package com.msdemo.v2.common.composite;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.composite.ProcessDefinition.TxnType;
import com.msdemo.v2.common.composite.chain.AbsProcessInterceptor;
import com.msdemo.v2.common.composite.chain.InterceptorChain;
import com.msdemo.v2.common.composite.flow.AbstractFlow;
import com.msdemo.v2.common.composite.flow.listeners.CompositeFlowListener;
import com.msdemo.v2.common.composite.handler.DefaultExceptionHandler;
import com.msdemo.v2.common.composite.handler.IExceptionHandler;
import com.msdemo.v2.common.composite.param.INewInstance;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.composite.param.ProcessResultMap;
import com.msdemo.v2.common.composite.spi.IResultWrapper;
import com.msdemo.v2.common.composite.transactional.ICompositeTxnContainer;
import com.msdemo.v2.common.generic.executor.IRemoteGenericExecutor;
import com.msdemo.v2.common.invocation.invoker.InvocationProxy;
import com.msdemo.v2.resource.management.SpringContextHolder;
import com.msdemo.v2.resource.script.spi.IScriptService;

@Component
public class CompositionFactory {

	private static final Logger logger =LoggerFactory.getLogger(CompositionFactory.class);
	
	private static Map<String,ProcessDefinition> catalog= new ConcurrentHashMap<>();
	private static ICompositeTxnContainer txnContainer;
	private static DefaultExceptionHandler defaultExceptionHandler = 
			new DefaultExceptionHandler();
	private static InterceptorChain defaultChain;
	private static InvocationProxy defaultInvoker = new InvocationProxy();
	private static IScriptService defaultScriptService;
	private static IRemoteGenericExecutor defaultGenericExecutor;
	private static CompositeFlowListener defaultFlowListener;
	
	@Autowired
	SpringContextHolder contextHolder;
	//to support auto-registration of ProcessFlow bean
	@Autowired
	@Nullable
	ProcessDefinition[] pfs;
		
	@Autowired
	@Nullable
	ICompositeTxnContainer availableContainer;	
	
	@Autowired
	@Nullable
	InterceptorChain compositionChain;
	
	@Autowired
	@Nullable
	CompositeFlowListener flowListener;
	
	@Autowired
	@Nullable
	IScriptService scriptService;
	
	@Autowired
	IRemoteGenericExecutor genericExecutor;
		
	@PostConstruct
	public void init() throws Exception {
		txnContainer=availableContainer;
		defaultChain=compositionChain!=null?compositionChain
				:new InterceptorChain();
		defaultScriptService=scriptService;
		defaultGenericExecutor=genericExecutor;
		defaultFlowListener =flowListener;
		
		//to support Process bean definition
		String name=null;
		try {
			for (ProcessDefinition definition:catalog.values()){	
				name=definition.getName();
				definition.apply(defaultChain,defaultScriptService,defaultFlowListener,false);
			}
		} catch (Exception e) {
			throw new RuntimeException(name+" failed, ",e);
		}	
		
		defaultGenericExecutor.onModelChange();
//		for (IRemoteGenericExecutor executor : SpringContextHolder.getContext().getBeansOfType(IRemoteGenericExecutor.class).values()){
//			if (!executor.equals(defaultGenericExecutor)) executor.onModelChange();
//		}
	}
	
	public static IScriptService getDefaultScriptService(){
		return defaultScriptService;
	}
	
	public static boolean isDeployed(String processName){
		return catalog.containsKey(processName);
	}
	public static ICompositeTxnContainer getTxnContainer(){
		return txnContainer;
	}
	public static String[] nameList(){
		return catalog.keySet().toArray(new String[0]);
	}
	
	//composite engine portal
	public static CompositionContext executeChain(String processName, Object requestDto){
		if (!isDeployed(processName)) throw new CompositionException(processName+" not defined");
		return get(processName).chain.getProcessChain().invoke(processName, requestDto);
	}
	
	public static CompositionContext executeTxnChain(String processName, Object requestDto){
		return get(processName).chain.getTxnChain().invoke(processName, requestDto);
	}
	public static CompositionContext executeTxnChain(CompositionContext context){
		return get(context.getProcessName()).chain.getTxnChain().invoke(context);
	}
	
	public static DefinitionBuilder process(String name){
		return new DefinitionBuilder(name);
	}
	
	//for dynamic deployment
	public static String deploy(ProcessDefinition definition,boolean refreshFlag){
		if (isDeployed(definition.name)){
			logger.info("upgraded process flow: {}", definition.name);
		}else{
			logger.info("add new process flow: {}", definition.name);
		}
		//verification required on hot deployment
		definition.apply(defaultChain,defaultScriptService,defaultFlowListener,refreshFlag);
		catalog.put(definition.name, definition);
		return definition.name;
	}
	
	public static ProcessDefinition get(String name){
		ProcessDefinition pf= catalog.get(name);
		if (pf==null)
			throw  new RuntimeException(name + " is undefined.");
		return pf;
	}
	
	public static IRemoteGenericExecutor getDefaultGenericExecutor() {
		return defaultGenericExecutor;
	}

	public static class DefinitionBuilder{
		private ProcessDefinition definition ;
		public DefinitionBuilder(String name){
			definition= new ProcessDefinition(name);
		}
		public DefinitionBuilder transaction(TxnType txnType){
			definition.txnType=txnType;
			return this;
		}
		public DefinitionBuilder invoker(InvocationProxy invoker){
			definition.invoker=invoker;
			return this;
		}
		public DefinitionBuilder scriptService(IScriptService scriptService){
			definition.scriptService=scriptService;
			return this;
		}
		/** order 越大：before后执行，after先执行；越小：before先执行，after后执行**/
		public DefinitionBuilder interceptor(AbsProcessInterceptor interceptor, int order){
			if (definition.interceptors==null) definition.interceptors=new ArrayList<>();
			definition.interceptors.add(new MutablePair<Integer,AbsProcessInterceptor>(order,interceptor));
			return this;
		}
		public DefinitionBuilder start(AbstractFlow flow){
			definition.flowList.add(flow);
			return this;
		}
		
		public DefinitionBuilder next(AbstractFlow flow){
			definition.flowList.add(flow);
			return this;
		}
		
		public DefinitionBuilder result(Class<? extends INewInstance> dtoClass,ParamMapping mapping){
			try {
				Object dto=dtoClass.newInstance();
				definition.resultObject = (INewInstance)dto;
			} catch (Exception e) {
				throw new CompositionException(e.getMessage());
			}				
			definition.resultMapping=mapping;
			return this;
		}
		public DefinitionBuilder result(IResultWrapper<?> wrapper){
			definition.resultWrapper=wrapper;
			return this;
		}
		public DefinitionBuilder resultMap(ParamMapping mapping){
			definition.resultObject=new ProcessResultMap();
			definition.resultMapping=mapping;
			return this;
		}
		public DefinitionBuilder exception(IExceptionHandler handler){
			definition.exceptionHandler=handler;
			return this;
		}
		public ProcessDefinition register(){
			return register(false);
		}
		public ProcessDefinition register(boolean replaceFlag){
			if (!replaceFlag){
				if (catalog.containsKey(definition.name))
					throw new RuntimeException("deplicate process: "+definition.name);
			}
			
			catalog.put(definition.name, definition);
			if (definition.exceptionHandler==null)
				definition.exceptionHandler=defaultExceptionHandler;
			if (definition.invoker==null){
				definition.invoker= defaultInvoker;
			}
			if (definition.scriptService==null)
				definition.scriptService=defaultScriptService;
			return definition;
		}
	}
	
	
}
