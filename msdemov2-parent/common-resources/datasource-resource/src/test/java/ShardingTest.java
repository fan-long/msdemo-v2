import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude={
		DruidDataSourceAutoConfigure.class,
		MybatisAutoConfiguration.class})
@ActiveProfiles({"test","sharding"})
@SpringBootTest(classes = { ShardingTest.class })
public class ShardingTest {

	@Autowired
	JdbcTemplate jdbc;
	
	@Test
	public void test(){
		jdbc.execute("insert into test(f1,f2,slice) values('a','a',1)");
		jdbc.execute("insert into test(f1,f2,slice) values('b','b',2)");
		jdbc.queryForList("select slice from test where slice=1");
		jdbc.queryForList("select slice from test where slice=2");
	}
}
