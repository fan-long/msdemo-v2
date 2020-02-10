package com.msdemo.v2.common.teller.verification;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.ManagedThreadLocal;
import com.msdemo.v2.common.param.service.BranchService;
import com.msdemo.v2.common.param.service.TellerService;
import com.msdemo.v2.common.param.table.entity.Branch;
import com.msdemo.v2.common.param.table.entity.Teller;

@Component
public class TellerVerificationHelper {

	//FIXME: example only, just apply to non-cached object. Teller and Branch are already cached by param-cache, 	
	private static final ManagedThreadLocal<Teller> cachedTeller
		= new ManagedThreadLocal<>(TellerVerificationHelper.class.getSimpleName()+".teller",Teller.class,false);
	private static final ManagedThreadLocal<Branch> cachedBranch
		= new ManagedThreadLocal<>(TellerVerificationHelper.class.getSimpleName()+".branch",Branch.class,false);
	
	@Autowired
	TellerService tellerService;
	
	@Autowired
	BranchService branchService;
	
	public static TellerService TELLER_SERVICE;
	public static BranchService BRANCH_SERVICE;
	
	@PostConstruct
	void init(){
		TELLER_SERVICE=tellerService;
		BRANCH_SERVICE=branchService;
	}

	public static void setCachedTeller(Teller teller){
		cachedTeller.set(teller);
	}
	public static Teller getCachedTeller() {
		return cachedTeller.get();
	}
	public static void setCachedBranch(Branch branch){
		cachedBranch.set(branch);
	}
	public static Branch getCachedBranch() {
		return cachedBranch.get();
	}
	
	public static void removeCahce(){
		cachedTeller.remove();
		cachedBranch.remove();
	}
}
