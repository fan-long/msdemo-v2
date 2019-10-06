package com.msdemo.v2.common.compose;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.compose.ProcessFlow.TxnType;
import com.msdemo.v2.common.compose.flow.AbstractFlow;
import com.msdemo.v2.common.compose.handler.DefaultExceptionHandler;
import com.msdemo.v2.common.compose.handler.IExceptionHandler;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.compose.trans.IComposeTxnContainer;

@Component
public class ProcessFlowFactory implements ApplicationContextAware{

	private static final Logger logger =LoggerFactory.getLogger(ProcessFlowFactory.class);
	
	private static ApplicationContext SpringContext;
	private static Map<String,ProcessFlow> Catalog= new ConcurrentHashMap<>();
	private static IComposeTxnContainer TxnContainer;
	private static DefaultExceptionHandler DefaultExceptionHandler = 
			new DefaultExceptionHandler();

	//to support auto-registration of ProcessFlow bean
	@Autowired
	@Nullable
	ProcessFlow[] pfs;
	
	@Autowired
	@Nullable
	IComposeTxnContainer availableContainer;
	
	
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringContext=applicationContext;	
		TxnContainer=availableContainer;
		//to support ProcessFlow bean definition
		for (ProcessFlow pf:Catalog.values()){			
				pf.verify();
		}		
	}
	
	public static Object getSpringBean(String beanName){
		return SpringContext.getBean(beanName);
	}
	public static ProcessFlowContext execute(String processName, Object req){
		ProcessFlow pf=ProcessFlowFactory.get(processName);
		switch (pf.txnType){			
			case Global:
				if (TxnContainer==null) {
					throw new RuntimeException("Global transaction container not found");
				}
				return TxnContainer.global(processName, req);
			case Local:
				if (TxnContainer==null) {
					throw new RuntimeException("Local transaction container not found");
				}
				return TxnContainer.local(processName, req);
			default:
				logger.debug("execute process flow [{}] without TxnContainer",processName);
				return pf.execute(req);
		}
	}
	
	public static ProcessFlowBuilder build(String name){
		return new ProcessFlowBuilder(name);
	}
	
	//for dynamic deployment
	public static void deploy(ProcessFlow pf){
		if (Catalog.containsKey(pf.name)){
			logger.info("upgrade process flow: {}", pf.name);
		}else{
			logger.info("add new process flow: {}", pf.name);
		}
		pf.verify();
		Catalog.put(pf.name, pf);
	}
	
	public static ProcessFlow get(String name){
		ProcessFlow pf= Catalog.get(name);
		if (pf==null)
			throw  new RuntimeException(name + " is undefined.");
		return pf;
	}
	
	public static class ProcessFlowBuilder{
		private ProcessFlow process ;
		public ProcessFlowBuilder(String name){
			process= new ProcessFlow(name);
		}
		public ProcessFlowBuilder transaction(TxnType txnType){
			process.txnType=txnType;
			return this;
		}
		public ProcessFlowBuilder start(AbstractFlow flow){
			process.flowList.add(flow);
			return this;
		}
		
		public ProcessFlowBuilder next(AbstractFlow flow){
			process.flowList.add(flow);
			return this;
		}
		
		public ProcessFlowBuilder result(Object dto,ParamMapping mapping){
			process.result=dto;
			process.resultMapping=mapping;
			return this;
		}
		public ProcessFlowBuilder resultMap(ParamMapping mapping){
			process.result=new LinkedHashMap<String,Object>();
			process.resultMapping=mapping;
			return this;
		}
		public ProcessFlowBuilder exception(IExceptionHandler handler){
			process.exeptionHandler=handler;
			return this;
		}
		public ProcessFlow register(){
			return register(false);
		}
		public ProcessFlow register(boolean replaceFlag){
			if (!replaceFlag){
				if (Catalog.containsKey(process.name))
					throw new RuntimeException("deplicate process: "+process.name);
			}
			Catalog.put(process.name, process);
			if (process.exeptionHandler==null)
				process.exeptionHandler=DefaultExceptionHandler;
			return process;
		}
	}
}
