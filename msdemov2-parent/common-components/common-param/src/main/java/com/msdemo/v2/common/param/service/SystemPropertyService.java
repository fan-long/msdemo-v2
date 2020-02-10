package com.msdemo.v2.common.param.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.param.consistent.IConsistentParamService;
import com.msdemo.v2.common.param.table.entity.SystemProperty;
import com.msdemo.v2.common.param.table.mapper.SystemPropertyMapper;

@Component
public class SystemPropertyService implements IConsistentParamService<SystemProperty>{

	@Autowired
	SystemPropertyMapper mapper;
	
	@Override
	public List<SystemProperty> findByLogicKey(SystemProperty criteria) {
//		SystemPropertyExample example = new SystemPropertyExample();
//		SystemPropertyExample.Criteria c=example.createCriteria();
//		c.andCodeEqualTo(criteria.getCode());
//		//NOTES: 如果使用参数缓存，缓存索引列应只配置逻辑主键,不应包含版本和日期字段
//		if (StringUtils.isNotEmpty(criteria.getVersion()))
//			c.andVersionEqualTo(criteria.getVersion());
//		if (StringUtils.isNotEmpty(criteria.getEffectiveDate()))
//			c.andEffectiveDateEqualTo(criteria.getEffectiveDate());
		return mapper.selectByLogicKey(criteria);
	}

	@Override
	public void doCreate(SystemProperty record) {
		mapper.insert(record);
	}

	@Override
	public void doUpdate(SystemProperty record) {
		mapper.insert(record);
	}

}
