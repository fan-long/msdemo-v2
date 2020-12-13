package com.msdemo.v2.resource.redis;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

public class RedisKeyExpirationMonitor implements MessageListener {
	
	private static final Logger logger =LoggerFactory.getLogger(RedisKeyExpirationMonitor.class);
	@Autowired
	RedisTemplate<String, Object> redis;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		logger.info("redis key: {} expired", expiredKey);
	}
	
	/**
	 * https://jira.spring.io/browse/DATAREDIS-534
	 * https://github.com/antirez/redis/issues/2541
	 * https://github.com/spring-projects/spring-session/issues/478
	 * 
	 * Redis keyspace notifications are only sent locally, so we should to
	 * create subscription on each node
	 */
	@PostConstruct
	public void init() throws Exception {
		RedisClusterConnection redisClusterConnection = redis.getConnectionFactory().getClusterConnection();
		// ((LettuceConnectionFactory)redisConnectionFactory).setShareNativeConnection(false);
		if (redisClusterConnection != null) {
			for (RedisClusterNode node : redisClusterConnection.clusterGetNodes()) {
				if (node.isMaster()) {
					RedisStandaloneConfiguration rsc = new RedisStandaloneConfiguration(node.getHost(), node.getPort());
					LettuceClientConfiguration.LettuceClientConfigurationBuilder lettuceClientConfigurationBuilder = LettuceClientConfiguration
							.builder();
					LettuceConnectionFactory factory = new LettuceConnectionFactory(rsc,
							lettuceClientConfigurationBuilder.build());
					factory.afterPropertiesSet();
					RedisMessageListenerContainer container = new RedisMessageListenerContainer();
					container.setConnectionFactory(factory);
					container.addMessageListener(this, new PatternTopic("__keyevent@*__:expired"));
					container.setTaskExecutor(new SimpleAsyncTaskExecutor(
							StringUtils.substringAfterLast(node.getHost(), ".") + ":" + node.getPort() + "-"));
					container.afterPropertiesSet();
					container.start();
				}
			}
		}
	}
}
