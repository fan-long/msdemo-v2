import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.v2.resource.datasource.TransactionManagerConfiguration;
import com.msdemo.v2.resource.transaction.BundledTransactionAspect;
import com.msdemo.v2.resource.transaction.BundledTransactional;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude={MybatisAutoConfiguration.class})
@ActiveProfiles({"test","datasource","mongodb"})
@SpringBootTest(classes = { BundledTransactionTest.class })
@Import({TransactionManagerConfiguration.class})
public class BundledTransactionTest {

	@Configuration 
	static class def{
		@Bean("mongoTransactionManager")
		MongoTransactionManager mongoTransactionManager(MongoDbFactory dbFactory) {
			return new MongoTransactionManager(dbFactory);
		}
		@Bean
		BundledTransactionAspect aspect(){
			return new BundledTransactionAspect();
		}
	}
	
	@Component
	public static class Service{
		@Autowired
		JdbcTemplate jdbc;
		
		@Autowired
		MongoTemplate mongo;
		
//		@Autowired
//		RabbitTemplate rabbit;
	//	
//		@Autowired
//		KafkaTemplate<String,String> kafka;
		
		public void jdbc(){
			jdbc.update("insert into test(id,name) values(1,'jdbc')");		
		}
		
		public static class Log{
			private String traceId;
			private BigDecimal balance;
			private String custNo;
			public String getTraceId() {
				return traceId;
			}
			public void setTraceId(String traceId) {
				this.traceId = traceId;
			}
			public BigDecimal getBalance() {
				return balance;
			}
			public void setBalance(BigDecimal balance) {
				this.balance = balance;
			}
			public String getCustNo() {
				return custNo;
			}
			public void setCustNo(String custNo) {
				this.custNo = custNo;
			}	
		}
		public void mongo(){
			for (int i=0;i<10;i++){
				Log log= new Log();
				log.setCustNo("cust_"+i);;
				log.setTraceId(UUID.randomUUID().toString());
				log.setBalance(new BigDecimal("0.01"));
				mongo.save(log);
			}			
		}
		
		@BundledTransactional({DataSourceTransactionManager.class,MongoTransactionManager.class})
		public void jdbcMongo(){
			mongo();
			jdbc();
		}
	}
	
	@Autowired
	Service service;
	
	@Test
	public void test(){
		service.jdbcMongo();
	}
}
