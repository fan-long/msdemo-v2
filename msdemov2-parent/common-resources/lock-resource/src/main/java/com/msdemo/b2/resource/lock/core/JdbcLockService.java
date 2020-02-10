package com.msdemo.b2.resource.lock.core;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.msdemo.b2.resource.lock.spi.IDistributionLock;
import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.util.LogUtils;

public class JdbcLockService implements IDistributionLock {
	
	@Value("${"+CommonConstants.CONFIG_ROOT_PREFIX+".lock.jdbc.table-name:t_lock}")
	String tableName;
		
	@Value("${"+CommonConstants.CONFIG_ROOT_PREFIX+".lock.retry-period:5}")
	int retryPeriod;
	
	@Autowired
	JdbcTemplate jdbc;
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String SQL_CREATE= "create table %s (%s varchar(32) not null, %s varchar(32) not null,"
			+ " %s DATETIME(3) not null, %s DATETIME(3) not null, %s varchar(128), %s varchar(512))";
	private static final String SQL_PK= "alter table %s add primary key(%s)";
	private static final String SQL_LOCK="insert into %s(%s,%s,%s,%s,%s,%s) values('%s','%s',DATE_ADD(now(3),INTERVAL %d*1000 MICROSECOND), now(3),'%s','%s')";
	private static final String SQL_DELETE= "delete from %s where %s='%s' and %s='%s'";
	private static final String SQL_FIND= "select * from %s where %s='%s'";
	private static final String SQL_RELOCK="update %s set %s=DATE_ADD(now(3),INTERVAL %d*1000 MICROSECOND) where %s='%s' and %s='%s'";
	
	@PostConstruct
	public void init() throws SQLException{
		ResultSet rs=jdbc.getDataSource().getConnection().getMetaData().getTables
			(null, null, tableName, null);
		if (!rs.next()){			
			jdbc.execute(String.format(SQL_CREATE, tableName, LockContext.LOCKKEY,LockContext.LOCKER,
					LockContext.EXPIRETIME,LockContext.CREATETIME,
					LockContext.CALLBACK,LockContext.COMMENTS));
			jdbc.execute(String.format(SQL_PK, tableName,LockContext.LOCKKEY));
			logger.info("lock table {} created", tableName);
		}
	}

	@Override
	public boolean lock(LockContext lock) {
		try{
			createLock(lock);
			return true;
		}catch(DuplicateKeyException e){
			LockContext existedLock=findLock(lock.getLockKey());
			if (existedLock!=null && existedLock.getLocker().equals(lock.getLocker()))
				//TODO: reset expire time
				return true;
			else
				return false;
		}catch(DataAccessException e){
			LogUtils.exceptionLog(logger, e);
			return false;
		}
	}

	@Override
	public boolean tryLock(LockContext lock, int waitMills) {
		int totalCount= waitMills/retryPeriod;
		int retryCount=0;
		long deadline=Instant.now().toEpochMilli() + waitMills;
		while(retryCount<=totalCount && Instant.now().toEpochMilli()<=deadline){
			try{
				createLock(lock);
				return true;
			}catch(Exception e){
				retryCount++;
				logger.debug("unable to lock [{}], retry #{} message: ",
						lock.getLockKey(),retryCount,e.getMessage());
				try {
					TimeUnit.MILLISECONDS.sleep(retryPeriod);
				} catch (InterruptedException e1) {
					LogUtils.exceptionLog(logger, e1);
				}
			}
		}			
		return false;
	}
	
	private void createLock(LockContext lock){
		jdbc.update(String.format(SQL_LOCK, tableName, 
				LockContext.LOCKKEY, LockContext.LOCKER,
				LockContext.EXPIRETIME,LockContext.CREATETIME,
				LockContext.CALLBACK,LockContext.COMMENTS,
				lock.getLockKey(),lock.getLocker(),lock.getTimeout(),
				StringUtils.isNotBlank(lock.getCallback())?lock.getCallback():"",
				StringUtils.isNotBlank(lock.getComments())?lock.getComments():""));			
	}
	
	@Override
	public boolean relock(LockContext lock) {
		try{
			int result=jdbc.update(String.format(SQL_RELOCK, tableName,
					LockContext.EXPIRETIME, lock.getTimeout(),
					LockContext.LOCKKEY, lock.getLockKey(),
					LockContext.LOCKER,lock.getLocker()));
			if (result!=1){
				if(result ==0)
					logger.warn("lock key [{}] not existed",lock.getLockKey());
				else
					//should never occurs
					logger.warn("{} records of lock key [{}] deleted",result,lock.getLockKey());	
				return false;
			}
			return true;
		}catch(Exception e){
			LogUtils.exceptionLog(logger, e);
			return false;
		}
	}

	@Override
	public boolean unlock(String key,String locker) {
		int result = jdbc.update(String.format(SQL_DELETE, tableName,
				LockContext.LOCKKEY,key,
				LockContext.LOCKER,locker));
		if (result!=1){
			if(result ==0)
				logger.warn("lock key [{}] with locker [{}] not existed ",key,locker);
			else
				//should never occurs
				logger.warn("{} records of lock key [{}] of locker [{}] deleted",result,key,locker);	
		}
		return true;
	}

	@Override
	public LockContext findLock(String key) {
		Connection conn=null;
		int defaultLevel=Connection.TRANSACTION_READ_COMMITTED;
		try {
			//should to get a new connection?
			conn= jdbc.getDataSource().getConnection();
			defaultLevel = conn.getTransactionIsolation();
			// read uncommitted 
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			Map<String,Object> lockMap=jdbc.queryForMap(String.format(SQL_FIND, tableName, LockContext.LOCKKEY,key));
			if (lockMap!=null){
				lockMap.put(LockContext.CREATETIME, ((Date)lockMap.get(LockContext.CREATETIME)).toInstant().toEpochMilli());
				lockMap.put(LockContext.EXPIRETIME, ((Date)lockMap.get(LockContext.EXPIRETIME)).toInstant().toEpochMilli());
				return LockContext.fromMap(lockMap);
			}
			else
				return null;
		}catch(Exception e){
			LogUtils.exceptionLog(logger, e);
			return null;
		}finally {
			if (conn!=null)
				try {
					conn.setTransactionIsolation(defaultLevel);
				} catch (SQLException e) {
					LogUtils.exceptionLog(logger, e);
				}
		}		
	}

	@Override
	public List<LockContext> topExpiredKeys(int count) {
		// TODO Auto-generated method stub
		return null;
	}

}
