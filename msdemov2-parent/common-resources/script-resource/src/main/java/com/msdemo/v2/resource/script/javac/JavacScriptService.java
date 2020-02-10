package com.msdemo.v2.resource.script.javac;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.core.IScriptService;
import com.msdemo.v2.common.core.IScriptStore;
import com.msdemo.v2.common.util.LogUtils;

public class JavacScriptService implements IScriptService {

	@Autowired
	IScriptStore store;
	
	@Value("${"+CommonConstants.CONFIG_ROOT_PREFIX+".script.javac.class-dir"+"}")
	String classDir;
	
	Logger logger =LoggerFactory.getLogger(getClass());

	private final static String PACKAGE=JavacScriptService.class.getPackage().getName()+".dynamic";
	//object for dynamic compile
	JavaCompiler javac = ToolProvider.getSystemJavaCompiler();  
	StandardJavaFileManager fm = javac.getStandardFileManager(null,null,Charset.defaultCharset());  
	private ClassLoader cl=null;
	private Map<String,IMapParamAwareScript> CachedClass = new ConcurrentHashMap<>();

	static class ScriptClassLoader extends ClassLoader{
		private static final String PACKAGE_SPLITTER=".";
		private static final String DIR_SPLITTER="/";
		private String classDir;
	    @Override
	    public Class<?> findClass(String name) {
	        String realPath = classDir + name.replace(PACKAGE_SPLITTER,DIR_SPLITTER) + ".class";
	        byte[] cLassBytes = null;
	        Path path = null;
	        try {
	            path = Paths.get(new URI(realPath));
	            cLassBytes = Files.readAllBytes(path);
	        } catch (IOException | URISyntaxException e) {
	            throw new RuntimeException("load class failed: "+e.getMessage());
	        }
	        Class<?> clazz = defineClass(name, cLassBytes, 0, cLassBytes.length);
	        return clazz;
	    }

	    public ScriptClassLoader(String classDir) {
	        this.classDir = "file:/".concat(classDir).concat(DIR_SPLITTER)
	        		.concat(PACKAGE.replaceAll(PACKAGE_SPLITTER,DIR_SPLITTER));
	    }
		
	}
	static class StringJavaObject extends SimpleJavaFileObject{  
	     //源代码  
	     private String sourceCode = "";  
	     private static final String CLASS_NAME_TEMPLATE="class ReplaceMe";
	     //遵循Java规范的类名及文件  
	     public StringJavaObject(String classFullName,String sourceCode){  
	           super(createStringJavaObjectUri(classFullName),Kind.SOURCE);  
	           this.sourceCode = "package " + PACKAGE+"; " + sourceCode.replace(CLASS_NAME_TEMPLATE, "class "+classFullName);  
	     }  
	     //产生一个URL资源路径  
	     private static URI createStringJavaObjectUri(String classFullName){  
	        return URI.create("String:///" + classFullName + Kind.SOURCE.extension);  
	     }  
	     
	     @Override  
	     public CharSequence getCharContent(boolean ignoreEncodingErrors)  
	            throws IOException {  
	        return sourceCode;  
	    }  
	}
	@PostConstruct
	public void init(){
		 cl = new ScriptClassLoader(classDir);
	}
	private String toClassName(String scriptId){
		return StringUtils.replace(scriptId,".", "_").replace("-", "_");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void cacheScript(String scriptId,String scriptContent) {
		long start =System.currentTimeMillis();
		String className= toClassName(scriptId);
		if (CachedClass.containsKey(scriptId))
			className=className.concat("_");
        JavaCompiler.CompilationTask task = 
        	javac.getTask(null, fm, null, 
        		Arrays.asList("-d",classDir),null,
        		Arrays.asList(new StringJavaObject(
        			className,scriptContent)));  
        if (task.call()){
			try {
				Class<IMapParamAwareScript> c= (Class<IMapParamAwareScript>) cl.loadClass(PACKAGE+"."+className);
				CachedClass.put(scriptId,c.newInstance());				
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw new RuntimeException("create instance failed: " +e.getMessage());
			}  
        }
        LogUtils.cost(logger, start, scriptId);
	}

	@Override
	public Object execute(String scriptId, Map<String, ?> context) {
		if (CachedClass.containsKey(scriptId)){
			CachedClass.get(scriptId).execute(context);
			return true;
		}else
			throw new RuntimeException(scriptId +" not defined");
	}

	@Override
	public Object executeScript(String scriptContent, Map<String, ?> context) {
		String id= "C"+String.valueOf(Math.abs(scriptContent.hashCode()));
		if (!CachedClass.containsKey(id))		
			cacheScript(id,scriptContent);
		return execute(id,context);	
	}

	@Override
	public Map<String, ?> scriptCache() {
		return CachedClass;
	}
	@Override
	public IScriptStore scriptStore() {
		return store;
	}

}
