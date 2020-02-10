package com.msdemo.v2.common.param.table.mapper;

import java.util.List;

import com.msdemo.v2.common.cache.core.CachedQuery;
import com.msdemo.v2.common.cache.core.ICachedParamTable;
import com.msdemo.v2.common.param.table.entity.Teller;

public interface TellerMapper extends ICachedParamTable<Teller>{
    int deleteByPrimaryKey(String tellerId);

    int insert(Teller record);
    
    @CachedQuery("tellerId")
    Teller selectByPrimaryKey(String tellerId);

    List<Teller> selectAll();

    int updateByPrimaryKey(Teller record);
}