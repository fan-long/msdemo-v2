import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.resource.mongo.MongoClusterConfiguration;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class, 
		MongoDataAutoConfiguration.class,
		DataSourceAutoConfiguration.class})
@ActiveProfiles({"test","mongodb_cluster"})
@SpringBootTest(classes = { MongoClusterTest.class })
@Import({ MongoClusterConfiguration.class })
public class MongoClusterTest {

	Logger logger = LoggerFactory.getLogger(MongoClusterTest.class);
	@Resource(name="onlineMongoTemplate")
	MongoTemplate online;
	
	@Resource(name="batchMongoTemplate")
	MongoTemplate batch;
	
	@Test
	public void test(){
		DateTimeFormatter FORMATTER= DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
		int count=100000;
		long start = Instant.now().toEpochMilli();
		String date=ZonedDateTime.ofInstant ( 
				Instant.ofEpochMilli(start),ZoneOffset.ofHours(+8)).format(FORMATTER);
		for (int i=0;i<count;i++){
			TransContext.Context context= new TransContext.Context();
			context.setTraceId(String.valueOf(i));
			context.setTransDate(date);
			batch.save(context);
//			online.save(context);
		}			
		logger.info("count: {}, cost: {}ms",count,Instant.now().toEpochMilli()-start);
	}
}
