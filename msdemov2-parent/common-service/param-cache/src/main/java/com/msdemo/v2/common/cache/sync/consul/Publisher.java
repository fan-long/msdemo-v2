package com.msdemo.v2.common.cache.sync.consul;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.ecwid.consul.v1.ConsulClient;
import com.msdemo.v2.common.cache.config.ParamCacheConstants;
import com.msdemo.v2.common.cache.core.CacheSyncDTO;
import com.msdemo.v2.common.cache.core.ICacheSyncPublisher;

public class Publisher implements ICacheSyncPublisher {

	@Value("${" + ParamCacheConstants.PREFIX_SYNC_CONSUL + "}")
	private String configPath;

	@Autowired
	private ConsulClient consulClient;

	@Override
	public void publish(List<CacheSyncDTO> modifiedCaches) {
		String version = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		for (CacheSyncDTO cache : modifiedCaches) {
			consulClient.setKVValue(configPath + cache.getCacheKey(), version);
		}
	}

}
