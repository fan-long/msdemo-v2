package com.msdemo.v2.resource.mybatis;

import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.context.TransContext;

@Component
@Intercepts(
  @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }
))
public class Support7x24Interceptor implements Interceptor {
	private static Logger logger = LoggerFactory.getLogger(Support7x24Interceptor.class);

	private boolean nightlyMode=TransContext.get().isNightlyMode();
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (nightlyMode){
			// 获取sql
			String sql = getSqlByInvocation(invocation);
			if (StringUtils.isBlank(sql)) {
				return invocation.proceed();
			}
			
			//sql类型
			String sqlType=getOperateType(invocation);
			logger.info("sql type: {}",sqlType);
			// 增加7*24逻辑
			String sql2Reset = sql;
	
			// 包装sql后，重置到invocation中
			resetSql2Invocation(invocation, sql2Reset);
		}
		// 返回，继续执行
		return invocation.proceed();
	}

	@Override
	public Object plugin(Object obj) {
		return Plugin.wrap(obj, this);
	}

	@Override
	public void setProperties(Properties arg0) {
		// doSomething
	}

	/**
	 * 获取sql语句
	 * 
	 * @param invocation
	 * @return
	 */
	private String getSqlByInvocation(Invocation invocation) {
		final Object[] args = invocation.getArgs();
		MappedStatement ms = (MappedStatement) args[0];
		Object parameterObject = args[1];
		BoundSql boundSql = ms.getBoundSql(parameterObject);
		return boundSql.getSql();
	}

	/**
	 * 包装sql后，重置到invocation中
	 * 
	 * @param invocation
	 * @param sql
	 * @throws SQLException
	 */
	private void resetSql2Invocation(Invocation invocation, String sql) throws SQLException {
		final Object[] args = invocation.getArgs();
		MappedStatement statement = (MappedStatement) args[0];
		Object parameterObject = args[1];
		BoundSql boundSql = statement.getBoundSql(parameterObject);
		MappedStatement newStatement = newMappedStatement(statement, new BoundSqlSqlSource(boundSql));
		MetaObject msObject = MetaObject.forObject(newStatement, new DefaultObjectFactory(),
				new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());
		msObject.setValue("sqlSource.boundSql.sql", sql);
		args[0] = newStatement;
	}

	private MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
		MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource,
				ms.getSqlCommandType());
		builder.resource(ms.getResource());
		builder.fetchSize(ms.getFetchSize());
		builder.statementType(ms.getStatementType());
		builder.keyGenerator(ms.getKeyGenerator());
		if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
			StringBuilder keyProperties = new StringBuilder();
			for (String keyProperty : ms.getKeyProperties()) {
				keyProperties.append(keyProperty).append(",");
			}
			keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
			builder.keyProperty(keyProperties.toString());
		}
		builder.timeout(ms.getTimeout());
		builder.parameterMap(ms.getParameterMap());
		builder.resultMaps(ms.getResultMaps());
		builder.resultSetType(ms.getResultSetType());
		builder.cache(ms.getCache());
		builder.flushCacheRequired(ms.isFlushCacheRequired());
		builder.useCache(ms.isUseCache());

		return builder.build();
	}

	private String getOperateType(Invocation invocation) {
		final Object[] args = invocation.getArgs();
		MappedStatement ms = (MappedStatement) args[0];
		SqlCommandType commondType = ms.getSqlCommandType();
		if (commondType.compareTo(SqlCommandType.SELECT) == 0) {
			return "select";
		}
		if (commondType.compareTo(SqlCommandType.INSERT) == 0) {
			return "insert";
		}
		if (commondType.compareTo(SqlCommandType.UPDATE) == 0) {
			return "update";
		}
		if (commondType.compareTo(SqlCommandType.DELETE) == 0) {
			return "delete";
		}
		return null;
	}

	// 定义一个内部辅助类，作用是包装sq
	class BoundSqlSqlSource implements SqlSource {
		private BoundSql boundSql;

		public BoundSqlSqlSource(BoundSql boundSql) {
			this.boundSql = boundSql;
		}

		@Override
		public BoundSql getBoundSql(Object parameterObject) {
			return boundSql;
		}
	}
}
