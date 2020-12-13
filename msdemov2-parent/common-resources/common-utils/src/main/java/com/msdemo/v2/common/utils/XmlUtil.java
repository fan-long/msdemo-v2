package com.msdemo.v2.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

public class XmlUtil {

	/**
	 * 格式化XML输出.
	 * @param xmlStr xml文本内容.
	 * @return
	 */
	public static String format(String xmlStr) throws RuntimeException{
		
		
		try (StringReader stringReader=new StringReader(xmlStr)){
			
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        InputSource is = new InputSource(stringReader);
	        Document doc = db.parse(is);
	        
	        return new String(format(doc));
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
		
	}

	/**
	 * 格式化输出.
	 * 
	 * @param node
	 * @return
	 */
	private static byte[] format(Node node) throws Exception {

		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("XML 3.0");
		LSSerializer serializer = impl.createLSSerializer();

		DOMConfiguration domConfiguration = serializer.getDomConfig();
		boolean isSupport = domConfiguration.canSetParameter("format-pretty-print", true);
		if (isSupport) {
			domConfiguration.setParameter("format-pretty-print", true);
		}

		LSOutput output = impl.createLSOutput();
		output.setEncoding(StandardCharsets.UTF_8.name());
		
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			output.setByteStream(byteArrayOutputStream);
			serializer.write(node, output);
			return byteArrayOutputStream.toByteArray();
		}

	}

}