package com.msdemo.v2.common.convert.definition.fix;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.convert.ConverterConfiguration;
import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IConverter;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.definition.IDefinitionHolder;

@Component
@ConfigurationProperties(prefix = ConverterConfiguration.CONFIG_ROOT) 
public class FixDefinitionHolder implements IDefinitionHolder<FixConverterContext> {

	public static final String DEFINITION_TYPE = "FIX";

	@Autowired
	ApplicationContext applicationContext;
	
	private Map<String,String> pathMap;
	
	private static final Map<String, IConverter<FixConverterContext>> cachedConverter = new ConcurrentHashMap<>();

	@PostConstruct
	void init() {
		saxParserFactory.setNamespaceAware(namespaceAware);
		saxParserFactory.setValidating(validating);		
	}

	private boolean namespaceAware = true;
	private boolean validating = false;

	static SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	public void setValidating(boolean validating) {
		this.validating = validating;
	}

	@Override
	public String name() {
		return DEFINITION_TYPE;
	}

	@Override
	public IRootConverter<FixConverterContext> fromFile(String definitionFileName) throws ConverterException {
		if (!cachedConverter.containsKey(definitionFileName)) {

			FixDefinitionXmlHandler handler = new FixDefinitionXmlHandler(this);
			SAXParser saxparser = null;

			synchronized (FixDefinitionHolder.class) {
				try {
					saxparser = saxParserFactory.newSAXParser();
				} catch (Exception e) {
					throw new ConverterException(e.getMessage());
				}
			}

			boolean fileNotFound=true;
			String fileNamePattern= ResourceLoader.CLASSPATH_URL_PREFIX+"%s%s.xml";
			try {
				for (String path: pathMap.values()){
					Resource r=applicationContext.getResource(String.format(fileNamePattern,path,definitionFileName));
					if (r.exists()){
						saxparser.parse(r.getInputStream(),	handler);
						fileNotFound=false;
						break;
					}
				}	
			} catch (Exception e) {
				throw new ConverterException(e.getMessage());
			}
			if (fileNotFound) 	
				throw new ConverterException(String.format("[%s.xml] not found from [%s]"
					,definitionFileName, String.join(",", pathMap.values()))); 

			cachedConverter.put(definitionFileName, handler.getRootConverter());
		}
		return (IRootConverter<FixConverterContext>)cachedConverter.get(definitionFileName);

	}

	public Map<String,String> getPathMap() {
		return pathMap;
	}

	public void setPathMap(Map<String,String> pathMap) {
		this.pathMap = pathMap;
	}

}
