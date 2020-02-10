package com.msdemo.v2.common.param.table.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.msdemo.v2.common.cache.core.CachedQuery;
import com.msdemo.v2.common.cache.core.ICachedParamTable;
import com.msdemo.v2.common.param.table.entity.SystemProperty;

public interface SystemPropertyMapper extends ICachedParamTable<SystemProperty>{
    int deleteByPrimaryKey(@Param("code") String code, @Param("version") String version, @Param("effectiveDate") String effectiveDate);

    int insert(SystemProperty record);
    
    @CachedQuery(value="code",dto=true)
    List<SystemProperty> selectByLogicKey(SystemProperty record);
    
    SystemProperty selectByPrimaryKey(@Param("code") String code, @Param("version") String version, @Param("effectiveDate") String effectiveDate);

    @Override
    List<SystemProperty> selectAll();

    int updateByPrimaryKey(SystemProperty record);
}