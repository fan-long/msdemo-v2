package com.msdemo.v2.common.cache.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(ParamCacheConstants.PREFIX_STORE)
public class ParamConfig {

	public static final String CACHEKEY_DISABLED="none";
	
	private HashMap<String,String> bakType;
	private HashMap<String,String> type;

	private CacheEnvHolder envHolder;
	
	public HashMap<String,String> getType() {
		return type;
	}

	/** TODO: Map结构的配置项运行时移除key无法被监听到,如果需要运行时关闭缓存，应把cache类型改为none **/
	public void setType(HashMap<String,String> newType) {
		if (this.type==null) {
			this.type=newType;
			bakType=new HashMap<>();
			bakType.putAll(this.type);
			return;
		}
		HashMap<String,String> currentType= this.bakType;
		Set<String> allCacheKey = new HashSet<>();
		allCacheKey.addAll(currentType.keySet());
		allCacheKey.addAll(newType.keySet());

		for (String cacheKey: allCacheKey ){
			if (currentType.containsKey(cacheKey)){
				if(!newType.containsKey(cacheKey)){
					//remove cache type
					envHolder.disableCache(cacheKey);					
				}else if (!currentType.get(cacheKey).equals(newType.get(cacheKey))){
					envHolder.changeCacheType(cacheKey, newType.get(cacheKey));
				}else{
					//same strategy type, nothing changed
				}
				
			}else if (newType.containsKey(cacheKey)){
				envHolder.enableCache(cacheKey, newType.get(cacheKey));
			}else
				envHolder.disableCache(cacheKey);
		}
		//this.type has already replaced by config watch thread
		this.type = newType;
		bakType.clear();
		bakType.putAll(this.type);
	} 
	
	public void linkToHolder(CacheEnvHolder holder){
		this.envHolder=holder;
	}
}
