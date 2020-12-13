package com.msdemo.v2.common.composite.journal;

import java.util.Map;

import com.msdemo.v2.common.invocation.journal.ServiceCallerJournal;

public class FlowJournal extends ServiceCallerJournal {
	private String flowName;

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}
	@Override
	public Map<String, Object> extendFieldToMap() {
		Map<String,Object> map= super.extendFieldToMap();
		map.put("flowName", flowName);
		return map;
	}
	@Override
	public void mapToExtendField(Map<String, Object> map) {
		super.mapToExtendField(map);
		this.flowName=map.get("flowName").toString();
	}
}
