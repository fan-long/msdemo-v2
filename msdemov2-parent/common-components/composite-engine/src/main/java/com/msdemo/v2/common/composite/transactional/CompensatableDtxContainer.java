package com.msdemo.v2.common.composite.transactional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.compensation.CompensatableTransactional;
import com.msdemo.v2.common.lock.model.ResourceLock.LockLevel;

@Component
public class CompensatableDtxContainer implements ICompositeTxnContainer {

	@Autowired
	@Nullable
	DataSource[] allDataSource;

	@Override
	@CompensatableTransactional(entry = true)
	public CompositionContext global(String processName, Object req,boolean allowLock) {
		if (allowLock)
			TransContext.get().common.getTxn().setLock(LockLevel.X.name());
		CompositionContext result = execute(processName, req);
		if (result.getException() != null)
			throw result.getException();
		return result;
	}

	@Transactional
	public CompositionContext local(String processName, Object req,boolean allowLock) {
		if (allowLock)
			TransContext.get().common.getTxn().setLock(LockLevel.S.name());
		CompositionContext result = execute(processName, req);
		if (result.getException() != null)
			throw result.getException();
		return result;
	}

	@Override
	@CompensatableTransactional(entry = true)
	public CompositionContext global(CompositionContext context, Object req,boolean allowLock) {
		if (allowLock)
			TransContext.get().common.getTxn().setLock(LockLevel.X.name());
		CompositionContext result = execute(context);
		if (result.getException() != null)
			throw result.getException();
		return result;
	}

	@Override
	@Transactional
	public CompositionContext local(CompositionContext context, Object req,boolean allowLock) {
		if (allowLock)
			TransContext.get().common.getTxn().setLock(LockLevel.S.name());
		CompositionContext result = execute(context);
		if (result.getException() != null)
			throw result.getException();
		return result;
	}

	@Override
	@Transactional
	public CompositionContext prepare(String processName, Object req) {
		try {
			return local(processName, req,false);
		} finally {
			for (DataSource ds : allDataSource) {
				ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(ds);
				if (holder != null) {
					holder.setRollbackOnly();
					break;
				}
			}
		}
	}

	@Override
	@Transactional
	public CompositionContext prepare(CompositionContext context, Object req) {
		try {
			return local(context, req,false);
		} finally {
			for (DataSource ds : allDataSource) {
				ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(ds);
				if (holder != null) {
					holder.setRollbackOnly();
					break;
				}
			}
		}
	}

}
