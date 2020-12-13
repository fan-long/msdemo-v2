package com.msdemo.v2.common.convert.tags.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.convert.core.ConverterException;
import com.msdemo.v2.common.convert.definition.fix.AbsFixContainer;
import com.msdemo.v2.common.convert.definition.fix.AbsFixConverter;
import com.msdemo.v2.common.convert.definition.fix.FixConverterContext;

@SuppressWarnings("unchecked")
public class LoopTag extends AbsFixContainer {

	private int maxCount =1;
	private int minCount =0;
	
	private String stopField;
	private String stopValue= StringUtils.EMPTY;
	
	
	
	public int getMaxCount() {
		return maxCount;
	}
	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}
	public int getMinCount() {
		return minCount;
	}
	public void setMinCount(int minCount) {
		this.minCount = minCount;
	}
	public String getStopField() {
		return stopField;
	}
	public void setStopField(String stopField) {
		this.stopField = stopField;
	}
	public String getStopValue() {
		return stopValue;
	}
	public void setStopValue(String stopValue) {
		this.stopValue = stopValue;
	}
	@Override
	public Object parse(FixConverterContext context) throws ConverterException{
		if(StringUtils.isEmpty(this.getName()))
			throw new ConverterException(
					String.format(ConverterException.REQUIRED_FIELD,
							this.getClass().getSimpleName(),"name"));
		
		List<Object> dtoList = new ArrayList<>();
		List<Map<String,Object>> mapList= new ArrayList<>();
		for (int i=0;i<maxCount;i++){
			//TODO: min count and stop field checking
			Map<String,Object> mapResult= new HashMap<String,Object>();
			for (AbsFixConverter converter : this.getChildren()) {
				String childName=converter.getName();
				Object value = converter.parse(context);
				mapResult.put(childName, value);
				logger.trace("{}-{}",converter.getName(),value);
			}		
			if (StringUtils.isNoneEmpty(this.getDto())) {
				try {
					dtoList.add(FixConverterContext.createDTO(getDto(), mapResult));
				} catch (Exception e){
					throw new ConverterException(
							String.format(ConverterException.DTO_INSTANCE,
									this.getDto(),e.getMessage()));
				};
				
			}
			mapList.add(mapResult);
		}
		context.set(this.getName(),mapList);
		return dtoList;
	}
	
	
	@Override
	public String format(Map<String,Object> src) throws ConverterException{
		StringBuilder sb= new StringBuilder();
		List<Map<String,Object>> loopList= (List<Map<String, Object>>) src.get(this.getName());
		for (int i=0;i<this.getMinCount();i++){
			logger.trace("{}:{}",this.getName(),i);
			Map<String,Object> map = Optional.ofNullable(loopList.get(i))
					.orElse(new HashMap<>());
			for (AbsFixConverter converter : this.getChildren()) {
				logger.trace("{}:{}",converter.getName(),map.get(converter.getName()));
				sb.append(converter.format(map));
			}
		}
		for(int i=this.getMinCount();i<this.getMaxCount();i++){			
			if (i>=loopList.size()) break;
			logger.trace("{}:{}",this.getName(),i);
			Map<String,Object> map=loopList.get(i);
			for (AbsFixConverter converter : this.getChildren()) {
				logger.trace("{}:{}",converter.getName(),map.get(converter.getName()));
				sb.append(converter.format(map));
			}
		}
		return sb.toString();
	}

}
