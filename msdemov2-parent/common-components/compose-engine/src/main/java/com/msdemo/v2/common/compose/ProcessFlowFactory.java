package com.msdemo.v2.common.compose;

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
import com.msdemo.v2.common.compose.param.INewInstance;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.compose.param.ProcessResultMap;
import com.msdemo.v2.common.compose.transaction.IComposeTxnContainer;
import com.msdemo.v2.common.util.LogUtils;

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
				pf.verify(false);
		}		
	}
	
	public static Object getSpringBeanByName(String beanName){
		return SpringContext.getBean(beanName);
	}
	public static Object getSpringBeanByType(String className){
		try {
			Class<?> type=Class.forName(className);
			String[] names=SpringContext.getBeanNamesForType(type);
			if (names.length>1) 
				throw new RuntimeException("multi bean found for class: "+className);
			else if(names.length==0)
				throw new RuntimeException("bean not found for class: "+className);
			return SpringContext.getBean(names[0]);
		} catch (BeansException | ClassNotFoundException e) {
			throw new RuntimeException("bean not defined for class: "+className);
		}
	}
	public static boolean isExisted(String processName){
		return Catalog.containsKey(processName);
	}
	public static String[] nameList(){
		return Catalog.keySet().toArray(new String[0]);
	}
	public static ProcessFlowContext execute(String processName, Object req){
		long start=System.currentTimeMillis();

		if (!isExisted(processName)) throw new ProcessFlowException(processName+" not defined");
		ProcessFlow pf=ProcessFlowFactory.get(processName);
		ProcessFlowContext result=null;
		switch (pf.txnType){			
			case Global:
				if (TxnContainer==null) {
					throw new RuntimeException("Global transaction container not found");
				}
				result= TxnContainer.global(pf.name, req);
				break;
			case Local:
				if (TxnContainer==null) {
					throw new RuntimeException("Local transaction container not found");
				}
				result= TxnContainer.local(pf.name, req);
				break;
			case Dynamic:
				logger.debug("execute process flow [{}], transaction type pending",pf.name);
				ProcessFlowContext context= pf.execute(req);
				if (context.isTxnTypeChanged()){
					context.removeChangeFlag();
					result= execute(context,req);
				}else
					result= context;
				break;
			case Prepare:
				if (TxnContainer==null) {
					throw new RuntimeException("Local transaction container not found");
				}
				try{
					result= TxnContainer.prepare(pf.name, req);
				}catch (Exception e) {
					logger.info("{} init completed and transaction rolled back",pf.name);
					result= null;
				}
				break;
			default:
				logger.debug("execute process flow [{}] without TxnContainer",pf.name);
				result= pf.execute(req);	
		}
		LogUtils.cost(logger, start, processName);

		return result;
	}
	
	public static ProcessFlowContext execute(ProcessFlowContext context,Object req){
		switch (context.getTxnType()){			
			case Global:
				if (TxnContainer==null) {
					throw new RuntimeException("Global transaction container not found");
				}
				return TxnContainer.global(context, req);
			case Local:
				if (TxnContainer==null) {
					throw new RuntimeException("Local transaction container not found");
				}
				return TxnContainer.local(context, req);			
			default:
				throw new RuntimeException(context.getTxnType()+ " not support on dynamic change");
		}
	}
	public static ProcessFlowBuilder build(String name){
		return new ProcessFlowBuilder(name);
	}
	
	//for dynamic deployment
	public static String deploy(ProcessFlow pf,boolean refreshFlag){
		if (Catalog.containsKey(pf.name)){
			logger.info("upgrade process flow: {}", pf.name);
		}else{
			logger.info("add new process flow: {}", pf.name);
		}
		//verification required on hot deployment
		pf.verify(refreshFlag);
		Catalog.put(pf.name, pf);
		return pf.name;
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
		
		public ProcessFlowBuilder result(String dtoClass,ParamMapping mapping){
			try {
				Object dto=Class.forName(dtoClass).newInstance();
				process.resultObject = (INewInstance)dto;
			} catch (Exception e) {
				throw new ProcessFlowException(e.getMessage());
			}				
			process.resultMapping=mapping;
			return this;
		}
		public ProcessFlowBuilder resultMap(ParamMapping mapping){
			process.resultObject=new ProcessResultMap();
			process.resultMapping=mapping;
			return this;
		}
		public ProcessFlowBuilder exception(IExceptionHandler handler){
			process.exceptionHandler=handler;
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
			if (process.exceptionHandler==null)
				process.exceptionHandler=DefaultExceptionHandler;
			return process;
		}
	}
	
	
}
