import java.time.Instant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.CompositionFactory;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.flow.SimpleFlow;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.composite.spi.IMappingWrapper;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.context.TransContext.Common;
import com.msdemo.v2.common.test.IParallelRunner;
import com.msdemo.v2.common.utils.LogUtil;

@RunWith(SpringRunner.class) 
@ContextConfiguration(classes={ComposePerformanceTest.class,CompositionFactory.class})
@Configuration
@SuppressWarnings("unused")
public class ComposePerformanceTest implements IParallelRunner {
	static final Logger logger = LoggerFactory.getLogger(ComposePerformanceTest.class);
	
	private static final String PROCESS_NAME="TESTPROCESS";
	
	public static class TestBean{
		public TransContext.Common invoke(TransContext.Common obj){
			obj.setDeadline(obj.getDeadline()+1);
			return obj;
		}
	}
	
	@Bean
	TestBean bean1(){
		return new TestBean();
	};
	
	@Autowired
	TestBean bean;
	
	public static class ComposeMapping implements IMappingWrapper{

		@Override
		public Object[] wrap(CompositionContext _context) {
			TransContext.Common context=(Common) _context.getReq();
			TransContext.Common context1 =new TransContext.Common();
			context1.setAcctDate(context.getAcctDate());
			context1.setDeadline(context.getDeadline());
			context1.getTxn().setDtxId(context.getTxn().getDtxId());
			context1.setTraceId(context.getTraceId());
			context.setTransDate(context1.getTransDate());
			context1.setAcctDate(context.getAcctDate());
			context1.setDeadline(context.getDeadline());
			context1.getTxn().setDtxId(context.getTxn().getDtxId());
			context1.setTraceId(context.getTraceId());
			context.setTransDate(context1.getTransDate());
			return new Object[]{context1};
		}
		
	}
	@Bean
	ProcessDefinition compose(){
		ParamMapping mapping= new ParamMapping()
				.add("traceId", "req.traceId")
				.add("deadline","req.deadline")
				.add("transDate","req.transDate")
				.add("acctDate","req.acctDate")
				.add("globalTxnId","req.globalTxnId")
				.add("traceId", "req.traceId")
				.add("deadline","req.deadline")
				.add("transDate","req.transDate")
				.add("acctDate","req.acctDate")
				.add("globalTxnId","req.globalTxnId")
				;
		IMappingWrapper wrapper = new ComposeMapping();
		SimpleFlow f1=SimpleFlow.builder().name("f1").bean(bean).method("invoke")
				.wrapper(wrapper).build();
		SimpleFlow f2=SimpleFlow.builder().name("f2").bean(bean).method("invoke")
				.wrapper(wrapper).build();
		SimpleFlow f3=SimpleFlow.builder().name("f3").bean(bean).method("invoke")
				.wrapper(wrapper).build();
		SimpleFlow f4=SimpleFlow.builder().name("f4").bean(bean).method("invoke")
				.wrapper(wrapper).build();
		SimpleFlow f5=SimpleFlow.builder().name("f5").bean(bean).method("invoke")
				.wrapper(wrapper).build();
		
		ProcessDefinition definition= CompositionFactory.process(PROCESS_NAME)
				.start(f1).next(f2).next(f3).next(f4).next(f5)
				.register();
		return definition;
	}
	
	public void logic(){
		int count=100000;
		long start= Instant.now().toEpochMilli();
//		for (int i=0;i<count;i++){
//			TransContext.CommonContext context =new TransContext.CommonContext();
//			bean.invoke(copyContext(context));
//			bean.invoke(copyContext(context));
//			bean.invoke(copyContext(context));
//			bean.invoke(copyContext(context));
//			bean.invoke(copyContext(context));
//		}
		for (int i=0;i<count;i++){
			CompositionFactory.get(PROCESS_NAME).executeObj(new TransContext.Common());
		}
		LogUtil.cost(null, start, "run("+count+")");
	}
	private TransContext.Common copyContext(TransContext.Common context){
		TransContext.Common context1 =new TransContext.Common();
		context1.setAcctDate(context.getAcctDate());
		context1.setDeadline(context.getDeadline());
		context1.getTxn().setDtxId(context.getTxn().getDtxId());
		context1.setTraceId(context.getTraceId());
		context.setTransDate(context1.getTransDate());
		context1.setAcctDate(context.getAcctDate());
		context1.setDeadline(context.getDeadline());
		context1.getTxn().setDtxId(context.getTxn().getDtxId());
		context1.setTraceId(context.getTraceId());
		context.setTransDate(context1.getTransDate());
		return context1;
	}
	
	@Test
	public void test(){
		logic();
		run(5);
		run(5);
		run(5);
		run(5);
		run(5);
	}
}
