package com.msdemo.b2.resource.lock.spi;

public interface IExpiredLockCallback {

	void onLockTimedout();
}
