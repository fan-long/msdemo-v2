package com.msdemo.v2.common.verification;

import com.msdemo.v2.common.verification.chain.IVerificationAware;

public interface IVerificationRule {
	 void check(IVerificationAware dto);
}
