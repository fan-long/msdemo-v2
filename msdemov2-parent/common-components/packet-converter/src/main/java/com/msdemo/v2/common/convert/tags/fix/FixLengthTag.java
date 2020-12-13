package com.msdemo.v2.common.convert.tags.fix;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.definition.fix.AbsFixConverter;
import com.msdemo.v2.common.convert.definition.fix.FixConverterContext;

public abstract class FixLengthTag extends AbsFixConverter {
	private boolean trim =true;
	private Align align=Align.right;
	private String fill=StringUtils.SPACE;
	
	private int length;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	abstract public Object parseValue(String srcValue);
	abstract public String formatValue(Object value);
	
	@Override
	public Object parse(FixConverterContext context){
		String byteValue= context.getAndForward(this.length);
		Object value=parseValue(byteValue);
		return value;
	}
	
	@Override
	public String format(Map<String,Object> map) throws ConverterException{
		String srcValue=formatValue(map.get(this.getName()));
		if (StringUtils.isNotEmpty(srcValue)){
			return fit(isTrim()?srcValue.trim():srcValue);
		}else
			return StringUtils.repeat(StringUtils.SPACE, this.length);

	}
	
	public boolean isTrim() {
		return trim;
	}

	public void setTrim(boolean trim) {
		this.trim = trim;
	}
	
	public Align getAlign() {
		return align;
	}

	public void setAlign(String align) {
		this.align = Align.valueOf(align);
	}

	public String getFill() {
		return fill;
	}

	public void setFill(String fill) {
		this.fill = fill;
	}

	protected String fit(String value){
		if (value.length()<= this.getLength())
			switch (this.getAlign()) {
				case left:
					return StringUtils.rightPad(value, this.length,this.getFill());
				default:
					return StringUtils.leftPad(value, this.length,this.getFill());
			}
		else
			return StringUtils.substring(value, 0, this.getLength());
	}
	protected static enum Align{
		left,right;
	}
}
