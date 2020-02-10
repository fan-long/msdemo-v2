package com.msdemo.v2.common.param.table.entity;

import com.msdemo.v2.common.cache.core.AbstractCachedObject;

public class Branch extends  AbstractCachedObject<Branch>{
    private String branchId;

    private String branchName;

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

	
}