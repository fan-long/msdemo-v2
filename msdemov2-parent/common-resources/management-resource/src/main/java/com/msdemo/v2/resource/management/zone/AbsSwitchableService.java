package com.msdemo.v2.resource.management.zone;

public abstract class AbsSwitchableService implements IManagedSwitchableService {

	private boolean activeFlag;
	private int zoneId=ZoneAwareResourceHolder.UNDEFINED_ZONE;
	
	@Override
	public boolean isActive() {
		return activeFlag;
	}

	@Override
	public void setZoneId(int zoneId) {
		this.zoneId=zoneId;		
	}

	@Override
	public int getZoneId() {
		return zoneId;
	}

	@Override
	public final void activate() {
		if (!this.activeFlag){
			this.activeFlag=true;
			this.doActivate();
		}
	}

	@Override
	public final void deactive() {
		if (this.activeFlag){
			this.doDeactive();
			this.activeFlag=false;
		}
	}
	
	protected abstract void doActivate();
	protected void doDeactive(){};

}
