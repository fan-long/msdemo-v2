package com.msdemo.v2.common.check.duplicate;

public interface IDuplicationChecker {
	
	void createCheckPoint(String key) throws DupCheckException;
	
	void updateStatus(String key,CheckPointStatus status);
	
	void removeCheckPoint(String key);

}
