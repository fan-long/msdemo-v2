package com.msdemo.v2.resource.trace.sleuth;

import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.sleuth.instrument.web.ClientSampler;
import org.springframework.cloud.sleuth.instrument.web.ServerSampler;
import org.springframework.cloud.sleuth.instrument.web.SkipPatternProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import brave.http.HttpAdapter;
import brave.http.HttpSampler;

/**
 * enable this configuration if want to remove Sleuth tracking for same endpoints  
 *
 */
//@Configuration
@ConditionalOnProperty("msdemo-config.sleuth")
@ConfigurationProperties(prefix = "msdemo-config.sleuth")
public class SleuthConfiguration {

	private List<String> filters;
	private static final Logger logger =LoggerFactory.getLogger(SleuthConfiguration.class);

	@Bean(name = ServerSampler.NAME)
	@Primary
	HttpSampler httpServerSampler(SkipPatternProvider provider) {
		Pattern pattern = provider.skipPattern();
		return new HttpSampler() {
			@Override
			public <Req> Boolean trySample(HttpAdapter<Req, ?> adapter, Req request) {
				String url = adapter.path(request);
				logger.debug(url);
				boolean shouldSkip = pattern.matcher(url).matches();
				if (shouldSkip) {
					return false;
				}
				return null;
			}
		};
	}

	@Bean
	@ClientSampler
	HttpSampler httpClientSampler() {
		return new HttpSampler() {
			@Override
			public <Req> Boolean trySample(HttpAdapter<Req, ?> adapter, Req request) {
				String url = adapter.path(request);
				logger.debug(url);
				if (filters.contains(url))
					return false;
				return null;
			}
		};
	}
	
	public List<String> getFilters() {
		return filters;
	}

	public void setFilters(List<String> filters) {
		this.filters = filters;
	}
}
