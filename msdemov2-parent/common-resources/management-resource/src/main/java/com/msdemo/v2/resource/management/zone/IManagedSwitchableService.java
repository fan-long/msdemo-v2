package com.msdemo.v2.resource.management.zone;

import java.util.Map;

import com.msdemo.v2.resource.management.SpringContextHolder;

public interface IManagedSwitchableService {

	boolean isActive();
	
	void setZoneId(int zoneId);
	int getZoneId();
	default String getZoneName(){ return (getZoneId()==ZoneAwareResourceHolder.UNDEFINED_ZONE)?"":String.valueOf(getZoneId());}
	
	@SuppressWarnings("unchecked")
	static <T extends IManagedSwitchableService> T getInstanceByZone(Class<T> clz){
		int zoneId = ZoneAwareResourceHolder.getBoundZoneId();	
		Map<String, ? extends IManagedSwitchableService> beans=SpringContextHolder.getContext().getBeansOfType(clz);
		if (beans!=null)
			for (IManagedSwitchableService t: beans.values()){
				if (t.getZoneId() == zoneId) return (T) t;
			}
		throw new IllegalArgumentException("instance of " +clz.getSimpleName() + " for [" +zoneId +"] not found");
	}
	
	void activate();
	void deactive();
}
