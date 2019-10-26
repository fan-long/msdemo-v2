package com.msdemo.v2.common.cache.core.sync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.msdemo.v2.common.cache.core.CacheEnvHolder;
import com.msdemo.v2.common.cache.core.ICacheSyncStrategy;
import com.msdemo.v2.common.cache.core.ParamCacheConstants;
import com.msdemo.v2.common.cache.core.store.InMemoryCacheStore;
import com.msdemo.v2.common.cache.core.store.RedisCacheStore;


public class ConsulAPISyncAdapter implements ICacheSyncStrategy {

	@Value("${"+ParamCacheConstants.PREFIX_SYNC_CONSUL+"}")
	private String configPath;
		
	@Autowired
	private ConsulClient consulClient;
	
	private Map<String,String> versionMap= new HashMap<>();
	private long currentConsulIndex =0;
	
	@Autowired
	CacheEnvHolder holder;
	
	@Override
	@Scheduled( fixedDelayString = "${"+ParamCacheConstants.PREFIX_SYNC_CONSUL_WATCH+"}")
	public void subscribe() {
		Response<List<GetValue>>  consulData=consulClient.getKVValues(configPath);
		if (consulData.getConsulIndex()!=currentConsulIndex){
			for (GetValue v: consulData.getValue()){
				String cacheKey= StringUtils.replace(v.getKey(),configPath,"");
				if (holder.isCacheKeyEnabled(cacheKey)){
					if(versionMap.containsKey(cacheKey) &&
						!StringUtils.equals(versionMap.get(cacheKey),v.getDecodedValue())){
							holder.getStrategy(cacheKey).refresh(cacheKey);
						}
				}
				versionMap.put(cacheKey, v.getDecodedValue());				
			}
			currentConsulIndex =consulData.getConsulIndex();
		}		
	}

	@Override
	public void publish(ArrayList<String> cacheKeys) {		
		String version=new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		for (String cacheKey: cacheKeys ){
			if (holder.isCacheKeyEnabled(cacheKey)){
				if (holder.getStrategy(cacheKey) instanceof RedisCacheStore){
					holder.getStrategy(cacheKey).clear(cacheKey);
				}else if (holder.getStrategy(cacheKey) instanceof InMemoryCacheStore){
					consulClient.setKVValue(configPath+cacheKey, version);					
				}
			}
		}
	}

}
