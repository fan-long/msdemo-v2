package com.msdemo.v2.common.compose.transaction;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.ProcessFlowFactory;

/**
 * SPI to support GlobalTransaction and LocalTransaction
 * @author LONGFAN
 *
 */
public interface IComposeTxnContainer {

	default ProcessFlowContext execute(String processName,Object req){
		return ProcessFlowFactory.get(processName).execute(req);
	}
	default ProcessFlowContext execute(ProcessFlowContext context,Object req){
		return ProcessFlowFactory.get(context.getProcessFlowName()).execute(context,req);
	}
	
	ProcessFlowContext global(String processName,Object req);
	ProcessFlowContext global(ProcessFlowContext context,Object req);
	
	ProcessFlowContext local(String processName,Object req);
	ProcessFlowContext local(ProcessFlowContext context,Object req);
	
	ProcessFlowContext prepare(String processName,Object req);
	ProcessFlowContext prepare(ProcessFlowContext context,Object req);
}
