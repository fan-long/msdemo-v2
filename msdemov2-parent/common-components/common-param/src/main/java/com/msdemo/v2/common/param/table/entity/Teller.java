package com.msdemo.v2.common.param.table.entity;

import com.msdemo.v2.common.cache.core.AbstractCachedObject;

public class Teller extends AbstractCachedObject<Teller> {
    private String tellerId;

    private String tellerName;

    private String locked;

    private String branchId;

    public String getTellerId() {
        return tellerId;
    }

    public void setTellerId(String tellerId) {
        this.tellerId = tellerId;
    }

    public String getTellerName() {
        return tellerName;
    }

    public void setTellerName(String tellerName) {
        this.tellerName = tellerName;
    }

    public String getLocked() {
        return locked;
    }

    public void setLocked(String locked) {
        this.locked = locked;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }
}