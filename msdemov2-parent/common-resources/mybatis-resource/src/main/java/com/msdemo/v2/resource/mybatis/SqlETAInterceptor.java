package com.msdemo.v2.resource.mybatis;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msdemo.v2.common.CommonConstants;

@RefreshScope
@Component
@Intercepts(value = {
        @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class }),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class }) })
public class SqlETAInterceptor  implements Interceptor {
	private static Logger logger = LoggerFactory.getLogger(SqlETAInterceptor.class);
	private static ObjectMapper om= new ObjectMapper();

	@Value("${"+CommonConstants.CONFIG_ROOT_PREFIX+".performance.sql-cost:false}")
	boolean sqlCostEnabled;
	
	@Value("${"+CommonConstants.CONFIG_ROOT_PREFIX+".debug.sql-param:false}")
	boolean sqlParamEnabled;
	
	@Override
    public Object intercept(Invocation inv) throws Throwable
    {
		if (sqlCostEnabled){
	        MappedStatement mappedStatement = (MappedStatement) inv.getArgs()[0];
	        String sqlId = mappedStatement.getId();
	
	        long start = System.nanoTime();
	        Object returnValue=null;

	        returnValue = inv.proceed();
	        	      		
	        long cost =TimeUnit.NANOSECONDS.toMillis(System.nanoTime() -start);
	        
	        if (sqlParamEnabled && inv.getArgs()[1]!=null)
				logger.info("[{}]_cost: [{}]ms, params: [{}]", sqlId,cost,om.writeValueAsString(inv.getArgs()[1]));
	        else
	        	logger.info("[{}]_cost: [{}]ms", sqlId,cost);
	
	        return returnValue;
        }else{
        	return inv.proceed();
        }
    }

    @Override
    public Object plugin(Object o)
    {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties p){}
    
}
