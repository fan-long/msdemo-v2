import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.context.TransContext;
import com.msdemo.v2.common.dtx.compensation.CompensatableTransactional;
import com.msdemo.v2.common.dtx.compensation.ICompensatable;
import com.msdemo.v2.common.dtx.core.StageContext;
import com.msdemo.v2.common.dtx.lock.ITxnLockAgent;
import com.msdemo.v2.common.dtx.lock.TxnLock;
import com.msdemo.v2.common.lock.config.DistributionLockConfiguration;
import com.msdemo.v2.common.lock.model.ResourceLock.LockLevel;
import com.msdemo.v2.common.timing.config.TimerConfiguration;
import com.msdemo.v2.common.utils.DateTimeUtil;
import com.msdemo.v2.common.utils.LogUtil;
import com.msdemo.v2.resource.redis.RedisTemplateConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { CompensationTest.class })
@EnableAutoConfiguration(exclude = { 
		MongoAutoConfiguration.class,MongoDataAutoConfiguration.class })
@ActiveProfiles({ "test", "redis","datasource","stream" })
@Configuration
@EnableAsync(proxyTargetClass=true)
@Import({ RedisTemplateConfiguration.class, DistributionLockConfiguration.class,TimerConfiguration.class })
@ComponentScan({"com.msdemo.v2.common"})
public class CompensationTest {

	@Autowired
	SagaTestService sagaService;
		
	private static Logger logger = LoggerFactory.getLogger(CompensationTest.class);

	private static final String TRANSACTION_ID=DateTimeUtil.dateTime();

	private static final int TIMEOUT= 5;
	private static final int TIMEOUT_MILLIS=TIMEOUT*1000;
	@Test
	public void saga() {
		TransContext.get().common.setTraceId(TRANSACTION_ID);
		TransContext.get().common.setDeadline(Instant.now().plusSeconds(TIMEOUT).toEpochMilli());
		MDC.put(CommonConstants.MDC_TRACE_ID, TransContext.get().common.getTraceId());
		sagaService.trySaga(1, -1, true);
	}

	private static final String POSITIVE_KEY = "POSITIVE_KEY";
	private static final String NEGATIVE_KEY = "NEGATIVE_KEY";

	@Component
	public static class SagaTestService {
		@Autowired
		PositiveService pService;
		@Autowired
		NegativeService nService;

		@CompensatableTransactional(entry = true, timeout = TIMEOUT)
		public void trySaga(int positive, int negative, boolean timeout) {
			long start = System.currentTimeMillis();
			pService.redisAdd(positive);
			nService.redisAdd(negative);
			pService.dbAdd();
			nService.dbAdd();
			if (timeout) {
				try {
					TimeUnit.SECONDS.sleep(TIMEOUT + 5);
				} catch (InterruptedException e) {
				}
			}
			LogUtil.cost(logger, start, "trySaga completed");
		}

	}

	@Component
	public static class PositiveService implements ICompensatable {
		@Autowired
		RedisTemplate<String, Integer> redis;
		
		@Autowired
		JdbcTemplate jdbc;
		
		@Autowired
		ITxnLockAgent txnLock;

		@Override
		@Transactional
		public void compensate(Object... args) {
			if (args!=null && args.length==1){
				int positive = (Integer) args[0];
				redis.boundValueOps(POSITIVE_KEY).increment(positive * -1);
			}else
				jdbc.execute("update test set id = id-1 where name='P'");
		}

		
		@CompensatableTransactional(compensatorClass = PositiveService.class)
		public void redisAdd(int positive) {
			txnLock.lock(StageContext.get().getDtxId(),POSITIVE_KEY, LockLevel.X, TIMEOUT_MILLIS);
			if (positive <= 0) {
				throw new RuntimeException("incorrect positive number: " + positive);
			}
			redis.boundValueOps(POSITIVE_KEY).increment(positive);
		}
		
		@Transactional
		@CompensatableTransactional(compensatorClass = PositiveService.class)
		public void dbAdd() {
			txnLock.lock(StageContext.get().getDtxId(),POSITIVE_KEY, LockLevel.X, TIMEOUT_MILLIS);
			jdbc.execute("update test set id = id+1 where name='P'");
		}
	}

	@Component
	public static class NegativeService implements ICompensatable {
		@Autowired
		RedisTemplate<String, Integer> redis;
		@Autowired
		JdbcTemplate jdbc;
		
		@Autowired
		TxnLock txnLock;

		@CompensatableTransactional(compensatorClass = NegativeService.class)
		public void redisAdd(int negative) {
			txnLock.lock(StageContext.get().getDtxId(),NEGATIVE_KEY, LockLevel.X, TIMEOUT_MILLIS);
			if (negative >= 0) {
				throw new RuntimeException("incorrect negative number: " + negative);
			}
			redis.boundValueOps(NEGATIVE_KEY).increment(negative);
		}
		
		@Transactional
		@CompensatableTransactional(compensatorClass = NegativeService.class)
		public void dbAdd() {
			txnLock.lock(StageContext.get().getDtxId(),NEGATIVE_KEY, LockLevel.X, TIMEOUT_MILLIS);
			jdbc.execute("update test set id = id+1 where name='N'");
		}
		
		@Override
		public void compensate(Object... args) {
			if (args!=null && args.length==1){
				int negative = (Integer) args[0];
				redis.boundValueOps(NEGATIVE_KEY).increment(negative * -1);
			}else
				jdbc.execute("update test set id = id-1 where name='N'");
		}
	}
	
	@After
	public void tearDown() throws Exception{
		logger.info("waiting for async to remove timeout monitor and unlock");
		TimeUnit.SECONDS.sleep(TIMEOUT*2);
	}
}
