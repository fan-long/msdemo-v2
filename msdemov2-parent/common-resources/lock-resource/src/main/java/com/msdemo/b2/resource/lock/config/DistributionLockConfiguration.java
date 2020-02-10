package com.msdemo.b2.resource.lock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msdemo.b2.resource.lock.core.JdbcLockService;
import com.msdemo.b2.resource.lock.core.RedisLockService;
import com.msdemo.b2.resource.lock.spi.IDistributionLock;
import com.msdemo.v2.common.CommonConstants;

@Configuration
public class DistributionLockConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(DistributionLockConfiguration.class);

	public static final String LOCK_CONFIG_PREFIX = CommonConstants.CONFIG_ROOT_PREFIX + ".lock";

	@ConditionalOnProperty(prefix = LOCK_CONFIG_PREFIX, name = "type", havingValue = "jdbc", matchIfMissing = true)
	@Bean
	public IDistributionLock jdbcLock() {
		logger.info("start JDBC distribution lock service");
		return new JdbcLockService();
	}

	@ConditionalOnProperty(prefix = LOCK_CONFIG_PREFIX, name = "type", havingValue = "redis")
	@Bean
	public IDistributionLock redisLock() {
		logger.info("start Redis distribution lock service");
		return new RedisLockService();
	}

}
