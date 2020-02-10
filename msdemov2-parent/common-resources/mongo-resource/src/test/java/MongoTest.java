import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.test.IParallelRunner;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude={
		DataSourceAutoConfiguration.class})
@ActiveProfiles({"test","mongodb"})
@SpringBootTest(classes = { MongoTest.class })
public class MongoTest implements IParallelRunner{

	Logger logger = LoggerFactory.getLogger(MongoTest.class);
	
	@Autowired
	MongoTemplate mongo;
	
	private long start;
	private static int THREAD_COUNT=5;
	private static int COUNT=10000;

	@Test
	public void test(){
		start = System.currentTimeMillis();
		run(THREAD_COUNT);
	}
	
	@Override 
	public void logic(){
		DateTimeFormatter FORMATTER= DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
		long start = Instant.now().toEpochMilli();
		String date=ZonedDateTime.ofInstant ( 
				Instant.ofEpochMilli(start),ZoneOffset.ofHours(+8)).format(FORMATTER);
		
		TransContext.Context context= new TransContext.Context();
		for (int i=0;i<COUNT;i++){
			context.setAcctDate("cust_"+i);;
			context.setTraceId(UUID.randomUUID().toString());
			context.setTransDate(date);	
			mongo.insert(context);
		}		
	}
	
	@After
	public void after(){
		logger.info("count: {}, cost: {}ms",THREAD_COUNT*COUNT,System.currentTimeMillis()-start);
	}
}
