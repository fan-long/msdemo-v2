import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.msdemo.v2.common.test.IParallelRunner;
import com.msdemo.v2.resource.redis.RedisTemplateConfiguration;
import com.msdemo.v2.resource.trace.id.ITransIdGenerator;
import com.msdemo.v2.resource.trace.id.generator.SnowFlakeIdGenerator;

import junit.framework.TestCase;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude={
		DataSourceAutoConfiguration.class,DruidDataSourceAutoConfigure.class,
		MybatisAutoConfiguration.class})
@ActiveProfiles({"test","redis"})
@Import({RedisTemplateConfiguration.class})
@SpringBootTest(classes = { TransIdTest.class })
public class TransIdTest implements IParallelRunner{

	private static final int ID_COUNT=260000;
	private static final int THREAD_COUNT=10;
	
	@Configuration
	static class def{
		@Bean
		SnowFlakeIdGenerator snowflake(){
			return new SnowFlakeIdGenerator();
		}
	}
	
	@Autowired
	ITransIdGenerator idGenerator;
	
	private ConcurrentHashMap<String,String> idSet= new ConcurrentHashMap<>(ID_COUNT,1);
	
	@Override
	public void logic(){
		for (int i=0;i<ID_COUNT/THREAD_COUNT;i++){
			String id= idGenerator.nextId();
			idSet.put(id, "");
//			logger.debug(id);
		}
	}
	
	private long start;
	@Test
	public void test(){
		start=System.currentTimeMillis();
		run(THREAD_COUNT);
	}
	
	@Test
	public void testFormatter(){
		DateTimeFormatter FORMATTER= DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
		logger.info("{},{},{}",start,Instant.now().toEpochMilli(),Instant.ofEpochMilli(start)); 
		logger.info("{}",ZonedDateTime.ofInstant ( 
				Instant.ofEpochMilli(start),ZoneOffset.ofHours(+8)).format(FORMATTER));		
	}
	
	@After
	public void check(){
		TestCase.assertEquals(ID_COUNT, idSet.size());
		logger.info("cost: {}",System.currentTimeMillis()-start);
	}
}
