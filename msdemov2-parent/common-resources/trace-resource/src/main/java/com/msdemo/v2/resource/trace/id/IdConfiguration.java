package com.msdemo.v2.resource.trace.id;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.resource.trace.id.generator.CuratorIdGenerator;
import com.msdemo.v2.resource.trace.id.generator.OracleIdGenerator;
import com.msdemo.v2.resource.trace.id.generator.RedisIdGenerator;
import com.msdemo.v2.resource.trace.id.generator.SleuthIdGenerator;
import com.msdemo.v2.resource.trace.id.generator.SnowFlakeIdGenerator;

@Configuration
public class IdConfiguration {

	private static final String TRANS_ID_TYPE_CONFIG = CommonConstants.CONFIG_ROOT_PREFIX+".trans-id.type";
    
	@Configuration
	@ConditionalOnProperty(value = TRANS_ID_TYPE_CONFIG, havingValue = "zookeeper", matchIfMissing = false)
	public class ZookeeperIdConfiguration {
		@Value("${spring.zookeeper.connect-string}")
		String connectString;

		@Value("${spring.zookeeper.node.trans-id}")
		String transIdNode;

		@Bean
		public CuratorFramework client() {
			CuratorFramework client = CuratorFrameworkFactory.builder().connectString(connectString)
					.retryPolicy(retryPolicy()).sessionTimeoutMs(10000).connectionTimeoutMs(5000).namespace("TEST")
					.build();

			client.start();
			return client;
		}

		@Bean
		public RetryPolicy retryPolicy() {
			return new ExponentialBackoffRetry(5, 10, 100);
		}

		@Bean
		public ITransIdGenerator idGenerator() {
			return new CuratorIdGenerator(client(), retryPolicy(), transIdNode);
		}
	}

	@Configuration
	@ConditionalOnProperty(value = TRANS_ID_TYPE_CONFIG, havingValue = "redis", matchIfMissing = false)
	public class RedisIdConfiguration {
		@Bean
		public ITransIdGenerator idGenerator() {
			return new RedisIdGenerator();
		}
	}

	@Configuration
	@ConditionalOnProperty(value = TRANS_ID_TYPE_CONFIG, havingValue = "oracle", matchIfMissing = false)
	public class OracleIdConfiguration {
		@Bean
		public ITransIdGenerator idGenerator() {
			return new OracleIdGenerator();
		}
	}

	@Configuration
	@ConditionalOnProperty(value = TRANS_ID_TYPE_CONFIG, havingValue = "sleuth", matchIfMissing = true)
	public class SleuthIdConfiguration {
		@Bean
		public ITransIdGenerator idGenerator() {
			return new SleuthIdGenerator();
		}
	}
	
	@Configuration
	@ConditionalOnProperty(value = TRANS_ID_TYPE_CONFIG, havingValue = "snowflake", matchIfMissing = true)
	public class SnowFlakeIdConfiguration {
		@Bean
		public ITransIdGenerator idGenerator() {
			return new SnowFlakeIdGenerator();
		}
	}
}
