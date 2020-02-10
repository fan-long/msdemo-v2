import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.b2.resource.lock.core.JdbcLockService;
import com.msdemo.b2.resource.lock.core.LockContext;
import com.msdemo.b2.resource.lock.spi.IDistributionLock;

import junit.framework.TestCase;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude = { 
		MybatisAutoConfiguration.class })
@ActiveProfiles({ "test", "datasource" })
@SpringBootTest(classes = { JdbcLockTest.class })
public class JdbcLockTest { 

	Logger logger = LoggerFactory.getLogger(JdbcLockTest.class);
	
	@Configuration
	static class def{
		@Bean
		IDistributionLock lockService(){
			return new JdbcLockService();
		}
	}

	@Autowired
	IDistributionLock lockService;

	LockContext lock = new LockContext("test",this.getClass().getSimpleName(),100,null,null);

	@Test
	public void lock() {
		TestCase.assertEquals(lockService.lock(lock),true);
	}
	
	@Test 
	public void relockAndUnlock(){
		lockService.relock(lock);
		lockService.unlock(lock.getLockKey(),lock.getLocker());
	}
	
}
