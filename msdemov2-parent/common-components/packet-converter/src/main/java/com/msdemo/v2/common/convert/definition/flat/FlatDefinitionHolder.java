package com.msdemo.v2.common.convert.definition.flat;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.convert.ConverterConfiguration;
import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IConverter;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.definition.IDefinitionHolder;
import com.msdemo.v2.common.convert.tags.flat.AbsFlatConverter;
import com.msdemo.v2.common.convert.tags.flat.FlatBigDecimalTag;
import com.msdemo.v2.common.convert.tags.flat.FlatDtoTag;
import com.msdemo.v2.common.convert.tags.flat.FlatStringTag;
import com.msdemo.v2.common.convert.tags.flat.FlatTagEnum;
import com.msdemo.v2.common.convert.tags.flat.FlatTimestampTag;

@Component
@ConfigurationProperties(prefix = ConverterConfiguration.CONFIG_ROOT+".flat") 
public class FlatDefinitionHolder implements IDefinitionHolder<FlatConverterContext> {

	public static final String DEFINITION_TYPE = "FLAT";
	
	private Map<String,String> pathMap;

	private static final Map<String, IConverter<FlatConverterContext>> cachedConverter = new ConcurrentHashMap<>();

	@Autowired
	ApplicationContext applicationContext;
	
	@Override
	public String name() {
		return DEFINITION_TYPE;
	}

	@Override
	public IRootConverter<FlatConverterContext> fromFile(String definitionFileName) throws ConverterException {
		if (!cachedConverter.containsKey(definitionFileName)) {
			boolean fileNotFound=true;
			String fileNamePattern= ResourceLoader.CLASSPATH_URL_PREFIX+"%s%s.xml";
			try {
				for (String path: pathMap.values()){
					Resource r=applicationContext.getResource(String.format(fileNamePattern,path,definitionFileName));
					if (r.exists()){
						try(InputStream definition=r.getInputStream()){
							cachedConverter.put(definitionFileName, parse(definition));
						}
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

		}
		return (IRootConverter<FlatConverterContext>)cachedConverter.get(definitionFileName);
	}

	public Map<String,String> getPathMap() {
		return pathMap;
	}

	public void setPathMap(Map<String,String> pathMap) {
		this.pathMap = pathMap;
	}

	private FlatRootConverter parse(InputStream definition) throws Exception{
		FlatRootConverter converter = new FlatRootConverter();
		DocumentBuilderFactory bbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		docBuilder = bbf.newDocumentBuilder();
		Document doc = docBuilder.parse(definition);
		Element root = doc.getDocumentElement();
		String delimiter=root.getAttribute("delimiter");
		converter.setDelimiter(StringUtils.isNotBlank(delimiter)?delimiter:CommonConstants.DEFAULT_CSV_DELIMITER);
		NodeList childs= root.getChildNodes();
		for (int i=0;i<childs.getLength();i++){
			Node iNode=childs.item(i);
			if (StringUtils.equals(iNode.getNodeName(),"request")){
				converter.setRequestClass(
						Class.forName(iNode.getAttributes().getNamedItem("class").getTextContent()));
				for (int j=0;j<iNode.getChildNodes().getLength();j++){
					parseNode(converter,iNode.getChildNodes().item(j),true);
				}
			}else if (StringUtils.equals(iNode.getNodeName(),"response")){
				for (int j=0;j<iNode.getChildNodes().getLength();j++){
					parseNode(converter,iNode.getChildNodes().item(j),false);
				}
			}
		}
		return converter;
	}
	private void parseNode(FlatRootConverter converter, Node node, boolean isRequest) throws Exception{
		if (node.getNodeType()==Node.TEXT_NODE) return;
		FlatTagEnum tagName= FlatTagEnum.byName(node.getNodeName());
		AbsFlatConverter<?> fieldConverter=null;
		switch (tagName){
			case FlatString:
				fieldConverter= new FlatStringTag();				
				break;
			case FlatBigDecimal:
				fieldConverter= new FlatBigDecimalTag();
				break;
			case FlatTimeStamp:
				fieldConverter= new FlatTimestampTag();
				((FlatTimestampTag)fieldConverter).setPattern(
						node.getAttributes().getNamedItem("pattern").getTextContent());
				break;	
			case FlatDto:
				fieldConverter = new FlatDtoTag();
				if (isRequest) ((FlatDtoTag)fieldConverter).setRequestClass(
						Class.forName(node.getAttributes().getNamedItem("class").getTextContent()));				
				((FlatDtoTag)fieldConverter).setDelimiter(converter.getDelimiter());
				for (int i=0;i<node.getChildNodes().getLength();i++){
					parseNode((FlatRootConverter) fieldConverter,node.getChildNodes().item(i),isRequest);
				}
				break;
			default:
		}
		fieldConverter.setName(node.getAttributes().getNamedItem("name").getTextContent());
		if (isRequest)
			converter.cachedRequestConverter.add(fieldConverter);
		else
			converter.cachedResponseConverter.add(fieldConverter);		
	}
}
