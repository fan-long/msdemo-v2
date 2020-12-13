package com.msdemo.v2.resource.management.debug;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.context.TransContext;

public class DebugUtil {
	
	public static final String DTX_TIMEOUT="DTX_TIMEOUT";
	public static final String DTX_RUNTIME_EXCEPTION="DTX_RUNTIME_EXCEPTION";
	
	public static void trigger(){
		String event=TransContext.get().common.getDebug();
		if (StringUtils.equalsIgnoreCase(event,DTX_TIMEOUT))
			try {
				TimeUnit.SECONDS.sleep(15);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		else if (StringUtils.equalsIgnoreCase(event,DTX_RUNTIME_EXCEPTION))
			throw new RuntimeException(DTX_RUNTIME_EXCEPTION);
	}
}
