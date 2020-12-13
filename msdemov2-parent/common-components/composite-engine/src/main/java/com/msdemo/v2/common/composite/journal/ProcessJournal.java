package com.msdemo.v2.common.composite.journal;

import java.util.Map;

import com.msdemo.v2.common.dtx.journal.DtxMainJournal;

public class ProcessJournal extends DtxMainJournal {

	private String application;
	private String transCode;
	
	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getTransCode() {
		return transCode;
	}

	public void setTransCode(String transCode) {
		this.transCode = transCode;
	}
	
	@Override
	public Map<String, Object> extendFieldToMap() {
		Map<String,Object> map=super.extendFieldToMap();
		map.put("application",this.application);
		map.put("transCode",this.transCode);
		return map;
	}

	@Override
	public void mapToExtendField(Map<String, Object> map) {
		super.mapToExtendField(map);
		this.application=(String) map.get("application");
		this.transCode=(String) map.get("transCode");
	}
}
