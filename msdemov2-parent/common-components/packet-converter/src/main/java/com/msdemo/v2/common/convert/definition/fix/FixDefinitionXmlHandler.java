package com.msdemo.v2.common.convert.definition.fix;

import java.util.EmptyStackException;
import java.util.Stack;

import org.springframework.beans.BeanWrapperImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IConverter;
import com.msdemo.v2.common.convert.definition.IDefinitionHolder;
import com.msdemo.v2.common.convert.definition.IDefinitionParserAware;
import com.msdemo.v2.common.convert.tags.fix.FixTagEnum;

public class FixDefinitionXmlHandler extends DefaultHandler {

	private AbsFixConverter rootConverter;
	private Stack<AbsFixConverter> stack;
	private IDefinitionHolder<FixConverterContext> parser;

	public FixDefinitionXmlHandler(IDefinitionHolder<FixConverterContext> parser) {
		this.parser = parser;
	}

	public void startDocument() {
		this.stack = new Stack<>();
	}

	public void endDocument() {
	}

	@SuppressWarnings("unchecked")
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		AbsFixConverter converterInstance;
		try {
			converterInstance = FixTagEnum.getClassByTagName(localName).newInstance();
		} catch (Exception e) {
			throw new ConverterException(e.getMessage());
		}

		BeanWrapperImpl converterInstanceWapper = new BeanWrapperImpl(converterInstance);

		if (attributes.getLength() > 0) {
			for (int index = 0; index < attributes.getLength(); index++) {
				converterInstanceWapper.setPropertyValue(attributes.getLocalName(index), attributes.getValue(index));
			}
		}

		try {
			AbsFixContainer parentConverter = (AbsFixContainer) this.stack.peek();
			converterInstance.setParent(parentConverter);
			parentConverter.addChild(converterInstance);
			if (converterInstance instanceof IDefinitionParserAware) {
				((IDefinitionParserAware<FixConverterContext>) converterInstance).setDefinitionParser(parser);
			}
		} catch (EmptyStackException e) {
			converterInstance.setParent(null);
		}

		this.stack.push(converterInstance);
	}

	public void endElement(String s, String s1, String s2) throws SAXException {
		this.rootConverter = this.stack.pop();
	}

	public IConverter<FixConverterContext> getRootConverter() {
		return rootConverter;
	}

}
