package com.msdemo.v2.resource.script.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.core.IScriptService;
import com.msdemo.v2.common.core.IScriptStore;
import com.msdemo.v2.resource.script.groovy.GroovyCompiledScriptService;
import com.msdemo.v2.resource.script.javac.JavacScriptService;
import com.msdemo.v2.resource.script.store.RedisScriptStore;

@Configuration
public class ScriptAutoConfiguration {
	private static final Logger logger =LoggerFactory.getLogger(ScriptAutoConfiguration.class);

	public static final String SCRIPT_CONFIG_PREFIX=
			CommonConstants.CONFIG_ROOT_PREFIX+".script";
	
	@ConditionalOnProperty(prefix = SCRIPT_CONFIG_PREFIX, 
			name = "type", 
			havingValue="javac",matchIfMissing = true)
	@Configuration
	static class JavacScript{
		@Bean
		public IScriptService javac(){
			logger.info("start dynamic script service by Javac");
			return new JavacScriptService();
		}
	}
	
	@ConditionalOnProperty(prefix = SCRIPT_CONFIG_PREFIX, 
			name = "type",	havingValue="groovy")
	@Configuration
	static class GroovyScript{
		@Bean
		public IScriptService groovyService(){
			logger.info("start dynamic script service by Groovy");
			return new GroovyCompiledScriptService();
		}
	}
	
	@ConditionalOnProperty(prefix = SCRIPT_CONFIG_PREFIX, 
			name = "store",	havingValue="redis",matchIfMissing = true)
	@Configuration
	static class RedisStore{
		@Bean
		public IScriptStore redisStore(){
			logger.info("start dynamic script store service by Redis");
			return new RedisScriptStore();
		}
	}
}
