package com.msdemo.v2.common.cache.sync.consul;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.ecwid.consul.v1.ConsulClient;
import com.msdemo.v2.common.cache.config.UniversalCacheConstants;
import com.msdemo.v2.common.cache.core.CacheSyncDTO;
import com.msdemo.v2.common.cache.core.ICacheSyncPublisher;
import com.msdemo.v2.common.utils.DateTimeUtil;


public class Publisher implements ICacheSyncPublisher {

	@Value("${" + UniversalCacheConstants.PREFIX_SYNC_CONSUL + "}")
	private String configPath;

	@Autowired
	private ConsulClient consulClient;

	@Override
	public void publish(List<CacheSyncDTO> modifiedCaches) {
		String version = DateTimeUtil.dateTime();
		for (CacheSyncDTO cache : modifiedCaches) {
			consulClient.setKVValue(configPath + cache.getCacheKey(), version);
		}
	}

}

