package com.msdemo.v2.common.dtx.journal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Transient;

import com.msdemo.v2.common.journal.model.AbsInvocationJournal;

public class DtxMainJournal extends AbsInvocationJournal<DtxMainJournal> {

	private boolean async;
	
	@Transient
	private List<AbsInvocationJournal<?>> sortedStageJournals;

	@Transient
	private DtxMainJournal compensationJournal;
	
	

	public List<AbsInvocationJournal<?>> getSortedStageJournals() {
		return sortedStageJournals;
	}

	public void setSortedStageJournals(List<AbsInvocationJournal<?>> sortedStageJournals) {
		this.sortedStageJournals = sortedStageJournals;
	}

	public DtxMainJournal getCompensationJournal() {
		return compensationJournal;
	}

	public void setCompensationJournal(DtxMainJournal compensationJournal) {
		this.compensationJournal = compensationJournal;
	}

	@Override
	public void mapToExtendField(Map<String, Object> map) {
		this.async=(Boolean)map.get("async");
	}

	@Override
	public Map<String, Object> extendFieldToMap() {
		HashMap<String,Object> map=new HashMap<>();
		map.put("async", this.async);
		return map;
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}
}
