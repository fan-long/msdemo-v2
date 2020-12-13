package com.msdemo.v2.common.composite.chain;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.transactional.ICompositeTxnContainer;

/**
 * 服务编排执行前及执行后处理逻辑拦截器 
 * @author LONGFAN
 *
 */
public abstract class AbsProcessInterceptor {

	private static Logger logger =LoggerFactory.getLogger(AbsProcessInterceptor.class);
	
    /**编排流程及事务开启之前执行**/
    protected void pre(InterceptorModel model){};
    
    /**编排流程及事务提交之后执行**/
    protected void post(InterceptorModel model){};
    
    /**
     * 传递责任
     */
    protected CompositionContext intercept(InterceptorModel model){
    	pre(model);
    	CompositionContext context;
    	if (model.isLastInterceptor()){
    		context= finalInvoke(model);
    		model.context=context;
    	}else
			context= model.nextInterceptor().intercept(model);
    	post(model);
    	return context;
    }
    
    protected CompositionContext finalInvoke(InterceptorModel model){
    	return invoke(model);
    }
    protected static CompositionContext invoke(InterceptorModel model){
    	CompositionContext result=null;
//		model.context=result;
		switch (model.definition.getTxnType()){			
			case Global:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Global transaction container not found");
				}
				result= model.txnContainer.global(model.definition.getName(), model.requestDto,true);
				break;
			case GlobalNoLock:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Global transaction container not found");
				}
				result= model.txnContainer.global(model.definition.getName(), model.requestDto,false);
				break;
			case Local:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Local transaction container not found");
				}
				result= model.txnContainer.local(model.definition.getName(), model.requestDto,true);
				break;
			case LocalNoLock:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Local transaction container not found");
				}
				result= model.txnContainer.local(model.definition.getName(), model.requestDto,false);
				break;
			case Dynamic:
				logger.debug("execute process flow [{}], transaction type pending",model.definition.getName());
				CompositionContext context= model.definition.executeObj(model.requestDto);
				if (context.isTxnTypeChanged()){
					context.removeChangeFlag();
					model.context=context;
					result= dynamicExecute(model);
				}else
					result= context;
				break;
			case Prepare:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Local transaction container not found");
				}
				try{
					result= model.txnContainer.prepare(model.definition.getName(), model.requestDto);
				}catch (Exception e) {
					logger.info("{} init completed and transaction rolled back",model.definition.getName());
					result= null;
				}
				break;
			default:
				logger.debug("execute process flow [{}] without TxnContainer",model.definition.getName());
				result= model.definition.executeObj(model.requestDto);	
		}
		return result;
    }
    
    private static CompositionContext dynamicExecute(InterceptorModel model){
		switch (model.context.getTxnType()){			
			case Global:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Global transaction container not found");
				}
				return model.txnContainer.global(model.context, model.requestDto,true);
			case GlobalNoLock:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Global transaction container not found");
				}
				return model.txnContainer.global(model.context, model.requestDto,false);
			case Local:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Local transaction container not found");
				}
				return model.txnContainer.local(model.context, model.requestDto,true);			
			case LocalNoLock:
				if (model.txnContainer==null) {
					throw new UnsupportedOperationException("Local transaction container not found");
				}
				return model.txnContainer.local(model.context, model.requestDto,false);						
			default:
				throw new UnsupportedOperationException(model.context.getTxnType()+ " not support on dynamic change");
		}
	}
    protected static class InterceptorModel{
    	ProcessDefinition definition;
    	Object requestDto;
    	CompositionContext context;
    	ICompositeTxnContainer txnContainer;
    	int index;
    	List<AbsProcessInterceptor> interceptors;
		public ProcessDefinition getDefinition() {
			return definition;
		}
	
		public Object getRequestDto() {
			return requestDto;
		}
	
		public CompositionContext getContext() {
			return context;
		}

		public ICompositeTxnContainer getTxnContainer() {
			return txnContainer;
		}
		public AbsProcessInterceptor nextInterceptor() {
			return this.interceptors.get(++index);
		}
		public boolean isLastInterceptor() {
			return index == interceptors.size()-1;
		}
    	
    } 
}
