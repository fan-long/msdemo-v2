package com.msdemo.v2.common.cache.sync.consul;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.msdemo.v2.common.cache.aspect.CacheQueryAspect;
import com.msdemo.v2.common.cache.config.CacheEnvHolder;
import com.msdemo.v2.common.cache.config.ParamCacheConstants;
import com.msdemo.v2.common.cache.core.CacheSyncDTO;
import com.msdemo.v2.common.cache.core.ICacheSyncSubscriber;

@ConditionalOnBean(CacheQueryAspect.class)
@ConditionalOnProperty(prefix = ParamCacheConstants.PREFIX, 
	name = ParamCacheConstants.PREFIX_SYNC_TYPE, havingValue="consul")
@Component
public class Subscriber implements ICacheSyncSubscriber,InitializingBean {

	@Value("${"+ParamCacheConstants.PREFIX_SYNC_CONSUL+"}")
	private String configPath;
		
	private Map<String,String> versionMap= new HashMap<>();
	private long currentConsulIndex =0;
	
	@Autowired
	ConsulClient consulClient;
	
	@Autowired
	CacheEnvHolder holder;
	
	/**
	 * initialized by CacheEnvHolder, so only load cache version without refresh cache data
	 */
	@Override
	public void afterPropertiesSet() {
		loadCacheVersion(false);
	}
	
	/**
	 * watching cache refresh notification
	 */
	@Scheduled( fixedDelayString = "${"+ParamCacheConstants.PREFIX_SYNC_CONSUL_WATCH+"}")
	public void watch() {
		loadCacheVersion(true);
	}

	private void loadCacheVersion(boolean refreshFlag){
		Response<List<GetValue>>  consulData=consulClient.getKVValues(configPath);
		if (consulData.getConsulIndex()!=currentConsulIndex){
			for (GetValue v: consulData.getValue()){
				String cacheKey= StringUtils.replace(v.getKey(),configPath,"");
				if (holder.isCacheKeyEnabled(cacheKey) //cached enabled
					&& refreshFlag //not initial load
					&& !StringUtils.equals(v.getDecodedValue(),versionMap.get(cacheKey))) //version changed
				{
					holder.getStrategy(cacheKey).refresh(cacheKey);					
				}
				versionMap.put(cacheKey, v.getDecodedValue());				
			}
			currentConsulIndex =consulData.getConsulIndex();
		}		
	}

	@Override
	public void subscribe(List<CacheSyncDTO> modifiedCaches) {
		loadCacheVersion(true);		
	}

}
