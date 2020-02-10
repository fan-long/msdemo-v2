package com.msdemo.v2.resource.redis;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;

@Configuration
public class RedisTemplateConfiguration {

	@Autowired
	private RedisProperties redisProperties;
	
	@Bean
	public GenericObjectPoolConfig<?> genericObjectPoolConfig(Pool properties) {
		GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
		config.setMaxTotal(properties.getMaxActive());
		config.setMaxIdle(properties.getMaxIdle());
		config.setMinIdle(properties.getMinIdle());
		if (properties.getTimeBetweenEvictionRuns() != null) {
			config.setTimeBetweenEvictionRunsMillis(properties.getTimeBetweenEvictionRuns().toMillis());
		}
		if (properties.getMaxWait() != null) {
			config.setMaxWaitMillis(properties.getMaxWait().toMillis());
		}
		return config;
	}
	
	@Bean(destroyMethod = "destroy")
	public LettuceConnectionFactory lettuceConnectionFactory() {
		
	    //开启 自适应集群拓扑刷新和周期拓扑刷新
	    ClusterTopologyRefreshOptions clusterTopologyRefreshOptions =  ClusterTopologyRefreshOptions.builder()
	    		// 开启全部自适应刷新
	            .enableAllAdaptiveRefreshTriggers() // 开启自适应刷新,自适应刷新不开启,Redis集群变更时将会导致连接异常
	            // 自适应刷新超时时间(默认30秒)
	            .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30)) //默认关闭开启后时间为30秒
	    		// 开周期刷新 
	    		.enablePeriodicRefresh(Duration.ofSeconds(20))  // 默认关闭开启后时间为60秒 ClusterTopologyRefreshOptions.DEFAULT_REFRESH_PERIOD 60  .enablePeriodicRefresh(Duration.ofSeconds(2)) = .enablePeriodicRefresh().refreshPeriod(Duration.ofSeconds(2))
	            .build();
		
	    // https://github.com/lettuce-io/lettuce-core/wiki/Client-Options
	    ClientOptions clientOptions = ClusterClientOptions.builder()
	            .topologyRefreshOptions(clusterTopologyRefreshOptions)
	            .build();
 
	    LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
				.poolConfig(genericObjectPoolConfig(redisProperties.getLettuce().getPool()))
				//.readFrom(ReadFrom.MASTER_PREFERRED)
				.clientOptions(clientOptions)
//				.commandTimeout(Duration.ofMillis(200)) //默认RedisURI.DEFAULT_TIMEOUT 60  
				.build();
	    
		List<String> clusterNodes = redisProperties.getCluster().getNodes();
		Set<RedisNode> nodes = new HashSet<RedisNode>();
		clusterNodes.forEach(address -> nodes.add(new RedisNode(address.split(":")[0].trim(), Integer.valueOf(address.split(":")[1]))));
		
		RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
		clusterConfiguration.setClusterNodes(nodes);
		clusterConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));
		//clusterConfiguration.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
		
		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(clusterConfiguration, clientConfig);
		// lettuceConnectionFactory.setShareNativeConnection(false); //是否允许多个线程操作共用同一个缓存连接，默认true，false时每个操作都将开辟新的连接
		// lettuceConnectionFactory.resetConnection(); // 重置底层共享连接, 在接下来的访问时初始化
		return lettuceConnectionFactory;
	}
	
	/**
     * redisTemplate 序列化使用的jdkSerializeable, 存储二进制字节码, 所以自定义序列化类
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
 
        // 使用Jackson2JsonRedisSerialize 替换默认序列化
        Jackson2JsonRedisSerializer<?> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
 
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
 
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
 
        // 设置value的序列化规则和 key的序列化规则
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        redisTemplate.setEnableTransactionSupport(false);
        return redisTemplate;
    }
}
