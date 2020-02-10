import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.v2.resource.script.config.ScriptAutoConfiguration;
import com.msdemo.v2.resource.script.groovy.ImmediateGroovyScript;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@ActiveProfiles({"test"})
@SpringBootTest(classes = { GroovyTest.class })
@Import(ScriptAutoConfiguration.class)
public class GroovyTest {
	Logger logger = LoggerFactory.getLogger(GroovyTest.class);
	@Configuration
	static class def{
		@Bean
		ImmediateGroovyScript groovyScriptService() {
			return new ImmediateGroovyScript();
		}
	}
	
	@Autowired
	ImmediateGroovyScript service;
	
	public static class TestObject{
		private String name;
		private String value;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		
	}
	@Test
	public void test(){
		HashMap<String,Object> map= new HashMap<>();
		map.put("test", "value");
		HashMap<String,Object> params = new HashMap<>();
		params.put("params",map);
		
		int count=100000;

		runJava(count);
//		runGroovy(count);
		runCompiledGroovy(count);
		runSpel(count);
		runReflect(count);
	}
	
//	private void runGroovy(int count){
//		String scriptContent = //"import GroovyTest$TestObject\n" +
////				"def testObject = new GroovyTest$TestObject()\n"+
//				"object.setName('testObject')\n"+
//				"object.setValue('testValue')\n"+
//				"object.setName('testObject')\n"+
//				"object.setValue('testValue')\n"+
//				"object.setName('testObject')\n"+
//				"object.setValue('testValue')\n"+
//				"object.getName()\n"+
//				"object.getValue()\n"+
//				"object.getName()\n"+
//				"object.getValue()\n"
////						"params.put('testObject',testObject)\n"+
////						"System.out.println(params.get('test'));"
//				;
//		TestObject testObject = new TestObject();
//		Binding binding = service.createBinding(testObject);
//		Script script=service.createScript("test", scriptContent,binding);
//		
//		long start = System.currentTimeMillis();
//		for (int i=0;i<count;i++){					
//			script.run();
//		}
//		logger.info("runGroovy cost: {}ms, value: {}" ,System.currentTimeMillis()-start,testObject.getValue());
//	}
	@SuppressWarnings("resource")
	private void runCompiledGroovy(int count){
		String scriptContent = "def main(object){"+
				"object.setName('testObject')\n"+
				"object.setValue('testValue')\n"+
				"object.setName('testObject')\n"+
				"object.setValue('testValue')\n"+
				"object.setName('testObject')\n"+
				"object.setValue('testValue')\n"+
				"object.getName()\n"+
				"object.getValue()\n"+
				"object.getName()\n"+
				"object.getValue()\n"+
//						"params.put('testObject',testObject)\n"+
//						"System.out.println(params.get('test'));"
				"}";
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class<?> clz=gcl.parseClass(scriptContent);
		try {
			GroovyObject groovyObject = (GroovyObject) clz.newInstance();
			TestObject testObject = new TestObject();
			Object[] args = {testObject};
			long start = System.currentTimeMillis();
			for (int i=0;i<count;i++){
				groovyObject.invokeMethod("main", args);
			}
			logger.info("runCompiledGroovy cost: {}ms, value: {}" ,System.currentTimeMillis()-start,testObject.getValue());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	private void runJava(int count){
		TestObject testObject = new TestObject();
		long start = System.currentTimeMillis();
		for (int i=0;i<count;i++){
			testObject.setName("testObject");
			testObject.setValue("testValue");
			testObject.setName("testObject");
			testObject.setValue("testValue");
			testObject.setName("testObject");
			testObject.setValue("testValue");
			testObject.getName();
			testObject.getValue();
			testObject.getName();
			testObject.getValue();
		}
		logger.info("runJava cost: {}ms" ,System.currentTimeMillis()-start);
	}
	
	private void runSpel(int count){
		TestObject testObject = new TestObject();
		testObject.setName("");
		testObject.setValue("");
        SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, this.getClass().getClassLoader());
        ExpressionParser parser = new SpelExpressionParser(config);
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("object", testObject);
        String getname = "name";
        String getvalue = "value";
        Expression getnameexpression = parser.parseExpression(getname);
        Expression getvalueexpression = parser.parseExpression(getvalue);
        long start = System.currentTimeMillis();
		for (int i=0;i<count;i++){
			getnameexpression.setValue(testObject,getnameexpression.getValue(testObject));
			getvalueexpression.setValue(testObject,getvalueexpression.getValue(testObject));
			getnameexpression.setValue(testObject,getnameexpression.getValue(testObject));
			getnameexpression.setValue(testObject,getnameexpression.getValue(testObject));
			getvalueexpression.setValue(testObject,"testValue");
		}
		logger.info("runSpel cost: {}ms, value: {}" ,System.currentTimeMillis()-start,testObject.getValue());
	}
	
	private void runReflect(int count){
		TestObject testObject = new TestObject();
		try {
			Method setNameMethod=testObject.getClass().getDeclaredMethod("setName", String.class);
			Method setValueMethod=testObject.getClass().getDeclaredMethod("setValue", String.class);
			Method getNameMethod=testObject.getClass().getDeclaredMethod("getName", new Class[]{});
			Method getValueMethod=testObject.getClass().getDeclaredMethod("getValue", new Class[]{});
	        long start = System.currentTimeMillis();

			for (int i=0;i<count;i++){
				setNameMethod.invoke(testObject, "testObject");
				setValueMethod.invoke(testObject, "testValue");
				setNameMethod.invoke(testObject, "testObject");
				setValueMethod.invoke(testObject, "testValue");
				setNameMethod.invoke(testObject, "testObject");
				setValueMethod.invoke(testObject, "testValue");
				getNameMethod.invoke(testObject, new Object[]{});
				getValueMethod.invoke(testObject, new Object[]{});
				getNameMethod.invoke(testObject, new Object[]{});
				getValueMethod.invoke(testObject, new Object[]{});
			}
			logger.info("runReflect cost: {}ms, value: {}" ,System.currentTimeMillis()-start,testObject.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
