package com.msdemo.v2.common.param.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.param.table.entity.Branch;
import com.msdemo.v2.common.param.table.mapper.BranchMapper;

@Component
public class BranchService {
	
	@Autowired
	BranchMapper mapper;
	
	public Branch selectById(String branchId){
		return mapper.selectByPrimaryKey(branchId);
	}

	@Transactional
	public Branch createBranch(String branchId,String branchName){
		Branch branch= new Branch();
		branch.setBranchId(branchId);
		branch.setBranchName(branchName);
		mapper.insert(branch);
		return branch;
	}
	
	@Transactional
	public Branch updateBranch(String branchId,String branchName){
		Branch branch= new Branch();
		branch.setBranchId(branchId);
		branch.setBranchName(branchName);
		mapper.updateByPrimaryKey(branch);
		return branch;
	}
	
	@Transactional
	public void deleteBranch(String branchId){
		mapper.deleteByPrimaryKey(branchId);
	}
}
