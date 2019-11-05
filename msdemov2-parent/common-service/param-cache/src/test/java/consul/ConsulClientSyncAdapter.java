package consul;

import com.msdemo.v2.common.cache.core.ICacheSyncSubscriber;

public abstract class ConsulClientSyncAdapter implements ICacheSyncSubscriber {

//	@org.springframework.beans.factory.annotation.Value("${"+ParamCacheConstants.PREFIX_SYNC_CONSUL+"}")
//	private String configPath;
//	
//	@org.springframework.beans.factory.annotation.Value("${spring.cloud.consul.host}")
//	private String host;
//	
//	@org.springframework.beans.factory.annotation.Value("${spring.cloud.consul.port}")
//	private int port;
//	
//	private Map<String,String> mapper= new HashMap<>();
//
//	@Autowired
//	ICacheStoreStrategy store;
//	
//	@Override
//	public void subscribe() {
//		Consul client = Consul.builder().withUrl("http://"+host+":"+port)
////				.withHostAndPort(HostAndPort.fromParts(host, port))
//				.build(); 
//
//		KVCache cache = KVCache.newCache(client.keyValueClient(), configPath);
//		cache.addListener(newValues -> {
//		    // Cache notifies all paths with "configPath" the root path
//		    // If you want to watch only "foo" value, you must filter other paths
//		    Optional<Value> newValue = newValues.values().stream()
//		            .filter(value -> value.getKey().equals(configPath))
//		            .findAny();
//
//		    newValue.ifPresent(value -> {
//		        // Values are encoded in key/value store, decode it if needed
//		        Optional<String> decodedValue = newValue.get().getValueAsString();
//		        decodedValue.ifPresent(v -> 
//		        	{System.out.println(String.format("Value is: %s", v));});
//		        
//		    });
//		});
//		cache.start();
//
//	}
//
//	@Override
//	public void publish(String[] cacheKeys) {
//		if (ArrayUtils.isNotEmpty(cacheKeys)){
//		Consul client = Consul.builder().withHostAndPort(HostAndPort.fromParts(host, port)).build(); 
//
//		for (String cacheKey: cacheKeys )
//			client.keyValueClient().putValue(configPath+cacheKey, System.currentTimeMillis()+"");
//		}
//	}
//
//	@PostConstruct
//	void startSubScribe(){
//		this.subscribe();
//	}
}
