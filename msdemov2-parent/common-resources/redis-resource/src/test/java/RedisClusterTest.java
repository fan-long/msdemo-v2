import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Configuration
@ActiveProfiles({ "test", "redis" })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class})
@SpringBootTest(classes = { RedisClusterTest.class })
public class RedisClusterTest {

	private static final Logger logger = LoggerFactory.getLogger(RedisClusterTest.class);
	@Autowired
	RedisTemplate<String, String> redis;

	@Test
	public void test() {
		redis.boundValueOps("a").set("value");
		for (int i = 0; i < 100; i++) {
			try {
				redis.boundValueOps("c").set("value");
				redis.boundValueOps("b").set("value");
			} catch (Exception e) {
				e.printStackTrace();
				if (i < 2)
					logger.info("{}", e.getMessage());
				else
					logger.info("retry failed.");
			}
		}
		logger.info("a: {}, b: {}, c: {}", redis.boundValueOps("a").get(), redis.boundValueOps("b").get(),
				redis.boundValueOps("c").get());
	}
	
	@Test
	public void routing(){
		 BoundHashOperations<String,String,String> op=redis.boundHashOps("Routing");
		 op.put("01:account1", "cust_001");
		 op.put("01:account2", "cust_002");
		 op.put("01:account3", "cust_071");
		 op.put("01:account11", "cust_890");
		 op.put("01:account12", "cust_910");
		 op.put("01:account13", "cust_919");
	}
}
