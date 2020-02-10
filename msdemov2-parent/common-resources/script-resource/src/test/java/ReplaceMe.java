import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msdemo.v2.resource.script.javac.IMapParamAwareScript;

public class ReplaceMe implements IMapParamAwareScript {

	Logger logger =LoggerFactory.getLogger(getClass());
	@Override
	public void execute(Map<String, ?> context) {
		logger.info("hello: {}",context.get("test"));

	}

}
