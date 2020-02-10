import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.msdemo.v2.common.core.IScriptService;
import com.msdemo.v2.common.core.IScriptStore;
import com.msdemo.v2.resource.script.javac.JavacScriptService;
import com.msdemo.v2.resource.script.store.RedisScriptStore;

@RunWith(SpringRunner.class)
@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@ActiveProfiles({ "test", "redis" })
@SpringBootTest(classes = { JavacTest.class })
public class JavacTest {

	Logger logger = LoggerFactory.getLogger(JavacTest.class);

	@Configuration
	static class def {
		@Bean
		IScriptService javacService() {
			return new JavacScriptService();
		}

		@Bean
		IScriptStore redisStore() {
			return new RedisScriptStore();
		}
	}

	@Autowired
	IScriptService service;

	@Test
	public void hello() {
		String scriptId = "test.n1";
		HashMap<String, String> context = new HashMap<>();
		service.loadAll();
		// logger.info("start to load script");
		// service.load(scriptId);
		// logger.info("end to load script");
		context.put("test", "aaa");
		service.execute(scriptId, context);
		service.reload(scriptId);
		context.put("test", "bbb");
		service.execute(scriptId, context);
		context.put("test", "ccc");
		service.execute(scriptId, context);
	}

	@Test
	public void create() throws IOException {
		int count = 1000;
		String content = FileUtils.readFileToString(
				new File("D:/Project/git-project/msdemov2-parent/common-resources/script-resource/src/test/java/ReplaceMe.java"),
				java.nio.charset.Charset.defaultCharset());
		for (int i = 1; i <= count; i++) {
			String className = "c_" + i;
			service.updateStore(className, content.replace("ReplaceMe", className));
		}
		long start =System.currentTimeMillis();
		service.loadAll();
		logger.info("total {}", System.currentTimeMillis()-start);
	}
}
