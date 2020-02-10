package com.msdemo.v2.resource.mongo;

import javax.annotation.Resource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.msdemo.v2.common.CommonConstants;

@ConditionalOnProperty(name = CommonConstants.CONFIG_ROOT_PREFIX +".mongodb.cluster", havingValue = "true")
@Configuration
public class MongoClusterConfiguration {

	static class Config {
		private String uri;
		private String database;
		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}
		public String getDatabase() {
			return database;
		}
		public void setDatabase(String database) {
			this.database = database;
		}
	}

	@ConfigurationProperties(prefix = CommonConstants.CONFIG_ROOT_PREFIX + ".mongodb.online")
	@Component("onlineMongoConfig")
	static class OnlineMongoDBConfig extends Config {
	}

	@ConfigurationProperties(prefix = CommonConstants.CONFIG_ROOT_PREFIX + ".mongodb.batch")
	@Component("batchMongoConfig")
	static class BatchMongoDBConfig extends Config {
	}

	@Resource(name="onlineMongoConfig")
	OnlineMongoDBConfig onlineConfig;
	@Resource(name="batchMongoConfig")
	BatchMongoDBConfig batchConfig;
	
	@Bean("onlineMongoFactory")
	public MongoDbFactory onlineMongoFactory() throws Exception {
		MongoClientURI uri = new MongoClientURI(onlineConfig.getUri());
		MongoClient mongoClient = new MongoClient(uri);
		MongoDbFactory dbFactory = new SimpleMongoDbFactory(mongoClient, onlineConfig.getDatabase());
		return dbFactory;
	}
	@Bean("batchMongoFactory")
	public MongoDbFactory batchMongoFactory() throws Exception {
		MongoClientURI uri = new MongoClientURI(batchConfig.getUri());
		MongoClient mongoClient = new MongoClient(uri);
		MongoDbFactory dbFactory = new SimpleMongoDbFactory(mongoClient, batchConfig.getDatabase());
		return dbFactory;
	}
	@Bean(name = "onlineMongoTemplate")
	@Primary
	public MongoTemplate onlineMongoTemplate() throws Exception {
		return new MongoTemplate(onlineMongoFactory());
	}

	@Bean(name = "batchMongoTemplate")
	public MongoTemplate batchMongoTemplate() throws Exception {
		return new MongoTemplate(batchMongoFactory());
	}
	
	@Bean("onlineMongoTransactionManager")
	@Primary
	MongoTransactionManager logMongoTransactionManager() throws Exception{
		return new MongoTransactionManager(onlineMongoFactory());
	}
	@Bean("batchMongoTransactionManager")
	MongoTransactionManager batchMongoTransactionManager() throws Exception{
		return new MongoTransactionManager(batchMongoFactory());
	}
}
