package com.msdemo.v2.common.param.table.mapper;

import java.util.List;

import com.msdemo.v2.common.cache.core.CachedQuery;
import com.msdemo.v2.common.cache.core.ICachedParamTable;
import com.msdemo.v2.common.param.table.entity.Branch;


public interface BranchMapper extends ICachedParamTable<Branch>{
    int deleteByPrimaryKey(String branchId);

    int insert(Branch record);
    
    @CachedQuery(value="branchId",cloned=false)
    Branch selectByPrimaryKey(String branchId); 

    @Override
    List<Branch> selectAll();

    int updateByPrimaryKey(Branch record);
}