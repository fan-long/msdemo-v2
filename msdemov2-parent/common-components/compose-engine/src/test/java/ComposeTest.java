import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msdemo.v2.common.compose.ProcessFlow;
import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.ProcessFlowFactory;
import com.msdemo.v2.common.compose.flow.AsyncFlow;
import com.msdemo.v2.common.compose.flow.ConditionFlow;
import com.msdemo.v2.common.compose.flow.ParallelFlow;
import com.msdemo.v2.common.compose.flow.SimpleFlow;
import com.msdemo.v2.common.compose.handler.XmlDefinitionHelper;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.util.LogUtils;

@RunWith(SpringRunner.class) 
@ContextConfiguration(classes={ComposeTest.class,ProcessFlowFactory.class})
@Configuration
public class ComposeTest{
	
	private static final Logger logger = LoggerFactory.getLogger(ComposeTest.class);
	
	private static final String PROCESS_NAME="TESTPROCESS";
	
	private static ObjectMapper om= new ObjectMapper();

	@Component("Async")
	public static class Async{
		public void print(String a) throws Exception{
			logger.info("async print: {}",a);
		}
	}
	
	@Component("Add")
	public static class Add{
		public long add(long x,long y){
			return x+y;
		}
	}
	
	@Component("Multiply")
	public static class Multiply{
		public long multiply(long x,long y){
			return x*y;
		}
	}
	@Component("Append")
	public static class Append{
		public String append(String x){
			return x+x;
		}
	}
	@Component("Abnormal")
	public static class Abnormal{
		public String abnormal() throws Exception{
			throw new Exception("error") ;
		}
	}
	
	static class TestDTO{
		private long data1;
		private long data2;
		private String data3;
		public TestDTO(long data1,long data2,String data3){
			this.data1=data1;
			this.data2=data2;
			this.data3=data3;
		}
		public long getData1() {
			return data1;
		}
		public void setData1(long data1) {
			this.data1 = data1;
		}
		public long getData2() {
			return data2;
		}
		public void setData2(long data2) {
			this.data2 = data2;
		}
		public String getData3() {
			return data3;
		}
		public void setData3(String data3) {
			this.data3 = data3;
		}		
		public String toString(){
			return "[data1] "+ data1+" [data2] "+data2+ " [data3] "+data3;
		}
	}
	
	@Bean
	public ProcessFlow testProcess(){
		ParamMapping m1= new ParamMapping().add("_0", "req.data1")
				.add("_1", "1");
		ParamMapping m2= new ParamMapping().add("_0", "req.data2")
				.add("_1", "10");
		ParamMapping m3= new ParamMapping().add("_0", "req.data3");
		
		ParamMapping m41= new ParamMapping().add("_0", "['F-1']")
				.add("_1", "1");
		ParamMapping m42= new ParamMapping().add("_0", "['F-2']")
				.add("_1", "10");
		ParamMapping m43= new ParamMapping().add("_0", "['F-3']");
		
		
		SimpleFlow f41=SimpleFlow.builder().name("F-4-1").beanName("Add").method("add").mapping(m41).build();
		SimpleFlow f42=SimpleFlow.builder().name("F-4-2").beanName("Multiply").method("multiply").mapping(m42).build();
		SimpleFlow f43=SimpleFlow.builder().name("F-4-3").beanName("Append").method("append").mapping(m43).build();
		
		ParamMapping resultMapping= new ParamMapping()
				.add("['data1']","['F-4-1']")
				.add("['data2']","['F-4-2']")
				.add("['data3']","['F-4-3']");
		
		AsyncFlow asyncFlow=AsyncFlow.builder().name("F-async")
		.beanName("Async").method("print").mapping(m43).build();
		
		ProcessFlow pf= ProcessFlowFactory.build(PROCESS_NAME)
				.start(SimpleFlow.builder().name("F-1")
						.beanName("Add").method("add").mapping(m1).build())
				.next(SimpleFlow.builder().name("F-2")
						.beanName("Multiply").method("multiply").mapping(m2).build())
				.next(SimpleFlow.builder().name("F-3")
						.beanName("Append").method("append").mapping(m3).build())
				.next(ConditionFlow.builder().name("cond1")
						.on("['F-3']=='aa'", asyncFlow)
						.on("['F-2']==1000", asyncFlow)
						.on("['F-1']==0", asyncFlow)
						.build())				
				.next(ParallelFlow.builder().name("F-4")
						.addFlow(f41).addFlow(f42).addFlow(f43)
//						.addFlow(SimpleFlow.builder().name("Error").beanName("Abnormal").method("abnormal").build())
						.build())
				//.result(new TestDTO(0,0,""), resultMapping)
				.resultMap(resultMapping)
				.register();
		
		return pf;
	}
	
	@Test
	public void printProcessFlow() throws Exception{
		logger.info("processflow json: {}",
				om.writerWithDefaultPrettyPrinter()
					.writeValueAsString(ProcessFlowFactory.get(PROCESS_NAME)));
		
	}
	@Test
	public void beanComponse() throws Exception{
				
		TestDTO req = new TestDTO(5,100,"a");
		long start=System.currentTimeMillis();		
		ParamMapping.parser.parseExpression("data1").getValue(req);
		LogUtils.cost(logger, start, "load LogUtil and SpelExpressionParser class");
		
		ProcessFlowContext context=ProcessFlowFactory.execute(PROCESS_NAME,req);

//		context.forEach((k,v)->{
//			logger.info("[{}]:{}",k,v);
//		});
		TimeUnit.SECONDS.sleep(5);
		if (context.getException()==null)
			logger.info("{}",context.getResp());
		else
			LogUtils.exceptionLog(logger, context.getException());
	}

	@Test
	public void el() throws Exception{
		beanComponse();
		beanComponse();
	}
	
	@Test
	public void fromXml() throws Exception{
		String xml=ProcessFlowFactory.get(PROCESS_NAME).toXml();
		logger.info(xml);
		ProcessFlow pf=XmlDefinitionHelper.fromXml(xml);
		logger.info(pf.toXml());
	}
	
	@Test
	public void toXml(){
		logger.info(ProcessFlowFactory.get(PROCESS_NAME).toXml());
	}
}
