package com.msdemo.v2.common.composite.transactional;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.CompositionFactory;

/**
 * SPI to support GlobalTransaction and LocalTransaction
 * @author LONGFAN
 *
 */
public interface ICompositeTxnContainer {

	default CompositionContext execute(String processName,Object req){
		return CompositionFactory.executeTxnChain(processName, req);
	}
	default CompositionContext execute(CompositionContext context){
		return CompositionFactory.executeTxnChain(context);
	} 
	
	CompositionContext global(String processName,Object req, boolean allowLock);
	CompositionContext global(CompositionContext context,Object req, boolean allowLock);
		
	CompositionContext local(String processName,Object req, boolean allowLock);
	CompositionContext local(CompositionContext context,Object req, boolean allowLock);
	
	CompositionContext prepare(String processName,Object req);
	CompositionContext prepare(CompositionContext context,Object req);
}
