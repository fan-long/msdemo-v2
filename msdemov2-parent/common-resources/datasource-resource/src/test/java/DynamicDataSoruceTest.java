import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.v2.resource.datasource.DynamicDataSourceConfiguration;

import junit.framework.TestCase;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude = { 
		MybatisAutoConfiguration.class })
@ActiveProfiles({ "test", "dynamicdatasource" })
@SpringBootTest(classes = { DynamicDataSoruceTest.class })
@Import({ DynamicDataSourceConfiguration.class })
public class DynamicDataSoruceTest { 

	Logger logger = LoggerFactory.getLogger(DynamicDataSoruceTest.class);
	@Autowired
	JdbcTemplate jdbc;

	@Test
	public void writeAndRead() {
		DynamicDataSourceConfiguration.DynamicDataSource.setDataSource("write");
		jdbc.update("insert into account_mapping (account_no,cust_no,data_slice)" + " values('account1','cust1',1)");
		DynamicDataSourceConfiguration.DynamicDataSource.setDataSource("read");
		List<Map<String, Object>> result = jdbc
				.queryForList("select * from account_mapping where account_no='account1'");
		TestCase.assertNotNull(result);
		logger.info("{}", result.get(0).get("account_no"));
	}
	
	
}
