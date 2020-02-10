package com.msdemo.v2.common.core;

import java.util.Map;

public interface IScriptService {
	default int taskCount(){return 10;};
	/**
	 * load all scripts and cache the compiled script instance 
	 */
	default void loadAll(){
		Map<String,String> allScripts=scriptStore().loadAll();
		
		allScripts.keySet().parallelStream().forEach( id ->
				cacheScript(id, allScripts.get(id)));
//		
//		int count=allScripts.size();
//		ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(count);
//		queue.addAll(allScripts.keySet());
//        ExecutorService executorService=Executors.newFixedThreadPool(taskCount());
//		List<FutureTask<Boolean>> tasks= new ArrayList<>(count);
//        for (int i=0;i<allScripts.size();i++){
//			Callable<Boolean> c = () -> { 
//				String id= queue.poll();
//				if (id!=null) cacheScript(id, allScripts.get(id));
//				return true;};
//			FutureTask<Boolean> task=new FutureTask<Boolean>(c);
//			executorService.submit(task);
//			tasks.add(task);
//		}
//      executorService.shutdown();
//        
//        tasks.forEach( task -> {
//			try {
//				task.get();
//			} catch (InterruptedException | ExecutionException e) {
//				throw new RuntimeException("compile script failed: " + e.getMessage());
//			}
//		});
	}
	
	/**
	 * load script and cache the compiled script instance 
	 */
	default void load(String scriptId){
		if (!scriptCache().containsKey(scriptId))
			reload(scriptId);
	}
	
	/**
	 * reload from script store by scriptId and replace the cached instance
	 * @param scriptId
	 */
	default void reload(String scriptId){
		cacheScript(scriptId, scriptStore().load(scriptId));		
	}
	
	/**
	 * update script store 
	 * @param scriptId
	 * @param scriptContent
	 */
	default void updateStore(String scriptId,String scriptContent){
		scriptStore().create(scriptId, scriptContent);
	}
	
	/** 
	 * cache script without update store
	 * @param scriptId
	 * @param scriptContent
	 */
	void cacheScript(String scriptId,String scriptContent);
	
	/**
	 * execute the cached script by scriptId
	 * @param scriptId
	 * @param context
	 * @return
	 */
	Object execute(String scriptId,Map<String,?> context);
	/**
	 * execute script without cache
	 * @param scriptContent
	 * @param context
	 * @return
	 */
	Object executeScript(String scriptContent,Map<String,?> context);
	
	Map<String,?> scriptCache();
	
	IScriptStore scriptStore();
}
