import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.test.IParallelRunner;
import com.msdemo.v2.common.util.LogUtils;

import io.shardingsphere.shardingjdbc.spring.boot.SpringBootConfiguration;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude={
		SpringBootConfiguration.class,
		MybatisAutoConfiguration.class})
@ActiveProfiles({"test","datasource"})
@SpringBootTest(classes = { DruidTest.class })
public class DruidTest implements IParallelRunner{

	@Autowired
	JdbcTemplate jdbc;
	
	private static AtomicInteger index= new AtomicInteger(0);
	@Test 
	public void pgTest(){
		logger.info("{}",jdbc.queryForObject("select count(1) from account_mapping", Integer.class));
	}
	
	@Test
	public void runTest(){
		run(5);
	}
		
	@Component
	static class TestService{
		@Autowired
		JdbcTemplate jdbc;
		
		@Transactional
		public void holdConnection(){
			jdbc.queryForList("select * from batch_message where msg_id=1 for update");
			try {
				TimeUnit.MINUTES.sleep(1);
			} catch (InterruptedException e) {
				LogUtils.exceptionLog(logger, e);
			}		
		}
		
		@Transactional
		public void createTable(){
			jdbc.execute("create table  test_"+index.addAndGet(1)+"( f1 varchar(20)"
					+ ",f2 varchar(10)"
					+ ",slice smallint"
					+ ",f4 int "
					+ ",f5 int "
					+ ",f6 int "
					+ ",f7 int "
					+ ",f8 int "
					+ ",f9 int "
					+ ",f10 int "
					+ ")");
		}
		@Transactional
		public void dropTable(){
			jdbc.execute("drop table  test_"+index.addAndGet(1));
		}
	}

	@Autowired
	TestService service;
	
	@Override
	public void logic(){
//		service.holdConnection();
		for (int i=0;i<500;i++){
//			service.createTable();
			service.dropTable();
		}
	}
}
