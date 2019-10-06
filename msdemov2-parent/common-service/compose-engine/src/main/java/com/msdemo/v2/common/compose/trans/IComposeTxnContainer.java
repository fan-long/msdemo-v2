package com.msdemo.v2.common.compose.trans;

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
	
	ProcessFlowContext global(String processName,Object req);
	
	ProcessFlowContext local(String processName,Object req);
}
