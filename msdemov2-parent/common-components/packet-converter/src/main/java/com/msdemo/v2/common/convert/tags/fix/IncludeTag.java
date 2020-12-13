package com.msdemo.v2.common.convert.tags.fix;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.definition.IDefinitionHolder;
import com.msdemo.v2.common.convert.definition.IDefinitionParserAware;
import com.msdemo.v2.common.convert.definition.fix.AbsFixContainer;
import com.msdemo.v2.common.convert.definition.fix.FixConverterContext;
@SuppressWarnings("unchecked")
public class IncludeTag extends AbsFixContainer implements IDefinitionParserAware<FixConverterContext> {

	private static final String DEFAULT_NAME="body";

	private String suffix="";

	private String linkField;
	
	private String file;
	
	private IDefinitionHolder<FixConverterContext> definition;

	@Override
	public void setDefinitionParser(IDefinitionHolder<FixConverterContext> parser) {
		this.definition=parser;
	}

	@Override
	public Object parse(FixConverterContext context) throws ConverterException{
		IRootConverter<FixConverterContext> includeConverter;
		if (StringUtils.isNotEmpty(linkField)){
			includeConverter=definition.fromFile(context.get(linkField)+suffix);		
		}else{
			includeConverter=definition.fromFile(file);		
		}
		Map<?,?> mapResult=(Map<?,?>)includeConverter.parse(context);
		context.set(this.getName(), mapResult);
		AbsFixContainer container=(AbsFixContainer)includeConverter;
		if (StringUtils.isNotEmpty(container.getDto())){	
			Map<String,Object> dtoResult= new HashMap<>();
			dtoResult.put(this.getName(),
					FixConverterContext.createDTO(container.getDto(),mapResult));
			return dtoResult;
		}
		
		return mapResult;
	}
	
	@Override
	public String format(Map<String,Object> src) throws ConverterException{
		IRootConverter<FixConverterContext> includeConverter;
		if (StringUtils.isNotEmpty(linkField)){
			includeConverter=definition.fromFile(FixConverterContext.nestedGet(src,linkField)+suffix);		
		}else{
			includeConverter=definition.fromFile(file);		
		}
		
		return includeConverter.format((Map<String,Object>)src.get(this.getName()));
	}
	
	public String getLinkField() {
		return linkField;
	}

	public void setLinkField(String linkField) {
		this.linkField = linkField;
	}
	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
	
	@Override
	public String getName(){
		return StringUtils.isNotEmpty(super.getName())?super.getName():DEFAULT_NAME;
	}

}
