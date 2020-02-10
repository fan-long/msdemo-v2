package com.msdemo.v2.resource.transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.msdemo.v2.common.util.LogUtils;

/**
 * 1.5PC transaction, manually commit/roll-back bundled transactions 
 * @author LONGFAN
 * @warn it's NOT JTA, should to consider inconsistent transaction issue if commit/roll-back failed 
 */
@Aspect
@Component
public class BundledTransactionAspect {

	private static final Logger logger= LoggerFactory.getLogger(BundledTransactionAspect.class);
	
	@Autowired
	PlatformTransactionManager[] tms;
	
	Map<Class<? extends PlatformTransactionManager>, PlatformTransactionManager> tmTypeMap= new HashMap<>();
	
	@PostConstruct
	void init(){
		for (PlatformTransactionManager tm:tms){
			tmTypeMap.put(tm.getClass(), tm);
		}
	}
	
	@Around("@annotation(transaction)")
	public Object doTransaction(ProceedingJoinPoint pjd, BundledTransactional transaction) throws Throwable {
		DefaultTransactionDefinition transDefinition = new DefaultTransactionDefinition();
        //通过注解参数传递设置事务的传播级别、超时参数
		transDefinition.setPropagationBehavior(transaction.propagation().value());
//        transDefinition.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRED);
		transDefinition.setTimeout(transaction.timeout());
		//Spring事务管理框架根据事务的传播机制判断是新建事务还是用当前已有的事务
        ArrayList<Pair<PlatformTransactionManager,TransactionStatus>> transStatusList= new ArrayList<>(transaction.value().length);
        for (Class<? extends PlatformTransactionManager> tmClass:transaction.value()){
        	PlatformTransactionManager tm= tmTypeMap.get(tmClass);
        	TransactionStatus transStatus = tm.getTransaction(transDefinition);
        	transStatusList.add(Pair.of(tm,transStatus));
        }
        try {
			Object result =pjd.proceed();
			for (int i=transStatusList.size()-1;i>=0;i--){
				transStatusList.get(i).getLeft().commit(transStatusList.get(i).getRight());
			}
			return result;
		} catch (Throwable ce) {
			for (int i=transStatusList.size()-1;i>=0;i--){
				try {					
					transStatusList.get(i).getLeft().rollback(transStatusList.get(i).getRight());
				} catch (Exception rbe) {
					LogUtils.exceptionLog(logger, rbe);
					//TODO: if TM committed, deal with roll-back exception fail over 
				}
			}
			throw ce;
		}
	}
}
