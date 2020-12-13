package com.msdemo.v2.resource.management.zone;

import org.jboss.logging.MDC;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.context.TransContext;

public interface ZoneAwareResourceHolder {

	public static final int UNDEFINED_ZONE=-1;
	
	public static void bindZoneId(int zoneId){
		TransContext.get().local.put(CommonConstants.LOCAL_BOUND_ZONE_ID,zoneId);
		applyZoneTrace();
	}
	
	public static int getBoundZoneId() {
		if (TransContext.get().local.containsKey(CommonConstants.LOCAL_BOUND_ZONE_ID))
			return (int) TransContext.get().local.get(CommonConstants.LOCAL_BOUND_ZONE_ID);
		else
			return UNDEFINED_ZONE;
	};
	
	public static boolean isZoneBound(){
		return getBoundZoneId()!=UNDEFINED_ZONE;
	}
	
	public static void applyZoneTrace(){
		MDC.put(CommonConstants.MDC_ZONE_ID, isZoneBound()?TransContext.get().local.get(CommonConstants.LOCAL_BOUND_ZONE_ID):"");		
	}
	
	public static String getRedisResourceKey(String prefix){
		if (TransContext.get().local.containsKey(CommonConstants.LOCAL_BOUND_ZONE_ID))
			return prefix.concat(CommonConstants.KEY_SPLITTER).concat(TransContext.get().local.get(CommonConstants.LOCAL_BOUND_ZONE_ID).toString());
		else
			return prefix;
	}
	
	public static String getStreamingKey(String topic,String suffix){
		return topic.concat("_").concat(suffix);
	}
}
