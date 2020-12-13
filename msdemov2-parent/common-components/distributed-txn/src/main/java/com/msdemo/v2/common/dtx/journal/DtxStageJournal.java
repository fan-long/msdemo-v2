package com.msdemo.v2.common.dtx.journal;

import java.util.Map;
import java.util.Set;

import com.msdemo.v2.common.dtx.lock.ITxnLockAgent.UnlockInfo;
import com.msdemo.v2.common.invocation.journal.ServiceCalleeJournal;
import com.msdemo.v2.resource.redis.ClassSerializeUtil;

public class DtxStageJournal extends ServiceCalleeJournal {

	private Set<UnlockInfo> locks;
		
	@Override
	public Map<String, Object> extendFieldToMap() {
		Map<String,Object> map=super.extendFieldToMap();
		if (locks!=null)
			map.put("locks", ClassSerializeUtil.serialize(locks));
		return map;
	}

	@Override
	public void mapToExtendField(Map<String, Object> map) {
		super.mapToExtendField(map);
		if (map.containsKey("locks"))
			this.locks=ClassSerializeUtil.deserialize(map.get("locks").toString());
	}
	public Set<UnlockInfo> getLocks() {
		return locks;
	}
	public void setLocks(Set<UnlockInfo> locks) {
		this.locks = locks;
	}
	
}
