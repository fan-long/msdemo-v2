package com.msdemo.v2.common.param.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.param.table.entity.Teller;
import com.msdemo.v2.common.param.table.mapper.TellerMapper;

@Component
public class TellerService {
	@Autowired
	TellerMapper mapper;
	
	
	public Teller selectById(String tellerId){
		return mapper.selectByPrimaryKey(tellerId);
	}

	@Transactional
	public Teller createTeller(Teller teller){
		mapper.insert(teller);
		return teller;
	}
	
	@Transactional
	public Teller updateTeller(Teller updatedTeller){
		mapper.updateByPrimaryKey(updatedTeller);
		return updatedTeller;
	}
	
	@Transactional
	public void deleteTeller(String tellerId){
		mapper.deleteByPrimaryKey(tellerId);
	}
}
