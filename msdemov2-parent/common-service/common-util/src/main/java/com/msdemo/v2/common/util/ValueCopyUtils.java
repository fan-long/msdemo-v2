package com.msdemo.v2.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;

/**
 * ReflectASM通过ASM的API直接生成符合Java虚拟机规范的Class字节流, 此工具类基于ReflectASM执行高效的对象属性复制
 * 采用缓存机制，线程安全
 * @author LONGFAN
 * WARNING: only use for value-object copy
 */
@SuppressWarnings("unchecked")
public final class ValueCopyUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(ValueCopyUtils.class);
	
	private static String BASE_PACKAGE =String.join(".", Arrays.copyOfRange(ValueCopyUtils.class.getPackage().getName()
			.split("\\."), 0, 3));
	/**
	 * Object[]缓存数组结构： 
	 * 0: MethondAccess对象
	 * 1: FieldAccess对象,
	 * 2: Map<String,Integer[]>
	 * 		key为field名称, 
	 * 		value:数组长度为2使用MethodAccess, [0]为getter索引，[1]为setter索引
				     数组长度为1使用FieldAccess
	 *TODO: 多层对象嵌套关系，包括数组、list，如
	 *  public class DTO1
	 *    private List<DTO2> field1 
	 *  copy到
	 *  public class DO1
	 *    private List<VO2>  field1
	 *  可以通过增加@ValueCopy(ignored=true)注解进行，把非嵌套对象复制完成后，单独对List栏位的DTO2和VO2对象进行复制
	 */
	private static Map<Class<?>, Object[]> asmMap = 
			new HashMap<Class<?>, Object[]>();  
	  
	public static void beanToBean(Object src, Object tgt) {  
		if (src ==null || tgt ==null) throw new RuntimeException("object is null");
    	Object[] srcAsm = Optional.ofNullable(asmMap.get(src.getClass()))
    			.orElseGet(() -> cache(src.getClass()));
        
    	Object[] tgtAsm = Optional.ofNullable(asmMap.get(tgt.getClass()))
    			.orElseGet(() -> cache(tgt.getClass()));
    	
    	if (srcAsm[2] ==null || tgtAsm[2] ==null){
    		logger.debug("REFLECT-IGNORED: src-{} is {}, tgt-{} is {}", 
    				src.getClass(),srcAsm[2], tgt.getClass(), tgtAsm[2]);
    		return ;
    	}
    	
    	MethodAccess srcMethondAccess = (MethodAccess)srcAsm[0];
    	MethodAccess tgtMethondAccess = (MethodAccess)tgtAsm[0];

    	FieldAccess srcFieldAccess = (FieldAccess)srcAsm[1];
    	FieldAccess tgtFieldAccess = (FieldAccess)tgtAsm[1];
    	
    	Map<String,Integer[]> srcFieldMap=  (Map<String,Integer[]>)srcAsm[2];
    	Map<String,Integer[]> tgtFieldMap=  (Map<String,Integer[]>)tgtAsm[2];
    	
        srcFieldMap.keySet().stream()
        	.filter(fieldName -> tgtFieldMap.containsKey(fieldName))
        	.collect(Collectors.toList())
        	.forEach( fieldName -> {
	        	Integer[] srcIndexArray = srcFieldMap.get(fieldName);
//	        	try {
	        		if (tgtFieldMap.get(fieldName).length == 2)
						tgtMethondAccess.invoke(tgt, tgtFieldMap.get(fieldName)[1],
								srcIndexArray.length == 2 ? srcMethondAccess.invoke(src, srcIndexArray[0])
										: srcFieldAccess.get(src, srcIndexArray[0]));	
	        		else
	        			tgtFieldAccess.set(tgt, tgtFieldMap.get(fieldName)[0],
								srcIndexArray.length == 2 ? srcMethondAccess.invoke(src, srcIndexArray[0])
										: srcFieldAccess.get(src,srcIndexArray[0]));
//				} catch (ClassCastException e) {
//						logger.debug("ignored field: {} due to {}",fieldName,e.getMessage());
//				}
        	});                     
    }  
  
    
    private static final String GETTER="get%s";
    private static final String SETTER="set%s";
    private static final String GETTER_BOOLEAN="is%s";
 // 缓存reflectasm生成的动态代理类
    private static Object[] cache(Class<?> clz) {  
        synchronized (clz) {  
        	if (asmMap.containsKey(clz)) return asmMap.get(clz);  
            MethodAccess methodAccess = null; 
            FieldAccess fieldAccess =null;
            List<Field> allFields=new ArrayList<>();
            Class<?> tempClz=clz;
            while (tempClz!=null && tempClz.getPackage().getName().startsWith(BASE_PACKAGE)){
            	allFields.addAll(Arrays.asList(tempClz.getDeclaredFields())); 
            	tempClz=tempClz.getSuperclass();
            }
            Map<String,Integer[]> fieldList = new HashMap<>();  
            for (Field field : allFields) {  
            	try {
//            		int isNested=0;
            		if (field.isAnnotationPresent(ValueCopy.class)){
            			if (field.getAnnotation(ValueCopy.class).ignored()) continue;
            			if (field.getAnnotation(ValueCopy.class).nested()) throw new RuntimeException("nested field: " + field+" not supported yet.");
            		}
					//private and not static
					if ((Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers()))  
					        && !Modifier.isStatic(field.getModifiers())) { 
						 if  (methodAccess==null)
							methodAccess=MethodAccess.get(clz);
					    String fieldName = StringUtils.capitalize(field.getName());                       
					    fieldList.put(field.getName(),new Integer[]{ 
					    		field.getType().equals(Boolean.class) || field.getType().getName().equals("boolean")
				    				?methodAccess.getIndex(String.format(GETTER_BOOLEAN,fieldName))
				    				:methodAccess.getIndex(String.format(GETTER,fieldName)),
					    		methodAccess.getIndex(String.format(SETTER,fieldName))}
					    		); 
					} //default
					else if (field.getModifiers()==0){
						if  (fieldAccess==null)
							fieldAccess=FieldAccess.get(clz);
					     fieldList.put(field.getName(),new Integer[]{fieldAccess.getIndex(field)});
					}
				} catch (IllegalArgumentException e) {
					//private field without SETTER or GETTER, ignored
					continue;
				}
            }  
            
           	asmMap.put(clz, new Object[]{methodAccess,fieldAccess,fieldList}); 
           	
           	if (logger.isDebugEnabled()){
           		logger.debug("REFLECT {} cached, fields: {}", clz.getName(),String.join(",", fieldList.keySet()));
           	}

            return asmMap.get(clz);  
        }  
    }  
    
    /**TODO: properties mapping for nested object **/
    public static void mapToBean(Map<String,?> src, Object tgt){
    	mapToBean(src,tgt,tgt.getClass());
    }
    public static void mapToBean(Map<String,?> src, Object tgt,Class<?> tgtClass){
    	Object[] asm = Optional.ofNullable(asmMap.get(tgtClass))
    			.orElseGet(() -> cache(tgtClass));
    	
    	MethodAccess methodAccess= (MethodAccess)asm[0];
		FieldAccess fieldAccess = (FieldAccess)asm[1];
		Map<String,Integer[]> fieldsMap = (Map<String,Integer[]>) asm[2];
		
    	for (String field: src.keySet()){
    		if (fieldsMap.containsKey(field)){
    			Integer[] index= fieldsMap.get(field);
    			if (index.length==2)  
    				methodAccess.invoke(tgt, index[1], src.get(field));
    			else
    				fieldAccess.set(tgt, index[0], src.get(field));
    		}
    	}
    }    
    
    
    public static HashMap<String, Object> groupByKeys(List<?> records, boolean isList,
    		String delimiter, String ... fields){
    	Class<?> clz=  records.get(0).getClass();
    	Object[] asm = Optional.ofNullable(asmMap.get(clz))
    			.orElseGet(() -> cache(clz)); 
    	HashMap<String, Object> result = new HashMap<>();
    	Map<String,Integer[]> fieldsMap = (Map<String,Integer[]>) asm[2];
    	MethodAccess methodAccess= (MethodAccess)asm[0];
    	for (int index=0;index<records.size();index++){
    		String[] keys = new String[fields.length];
    		for (int i=0;i<fields.length;i++){
    			keys[i]= methodAccess.invoke(records.get(index), fieldsMap.get(fields[i])[0]).toString();
    		}
    		String groupKey=String.join(delimiter, keys);
    		if (isList){
    			List<Integer> indexList= result.containsKey(groupKey)?(List<Integer>)result.get(groupKey): new ArrayList<Integer>();
    			indexList.add(index);
        		result.put(groupKey, indexList);
    		}else{
    			result.put(groupKey, index);
    		}
    	}
    	return result;
    }
    
    public static Map<String,Object> beanToMap(Object src){
    	if (src==null) return null;
    	Map<String,Object> result = new HashMap<>();
    	Object[] asm = Optional.ofNullable(asmMap.get(src.getClass()))
    			.orElseGet(() -> cache(src.getClass())); 
    	MethodAccess methodAccess= (MethodAccess)asm[0];
		FieldAccess fieldAccess = (FieldAccess)asm[1];
    	Map<String,Integer[]> keyMap= (Map<String,Integer[]>)asm[2];
    	
    	
    	
    	keyMap.forEach((key,index)->{
    		Object value= (index.length==2)?methodAccess.invoke(src, index[0]):fieldAccess.get(src, index[0]);
    		if (logger.isTraceEnabled()) logger.trace("{}:{}",key,value);
    		 if (value==null)
    			 result.put(key, value);
    		 else if(value instanceof String 
	             || value instanceof Boolean 
                 || value instanceof Short 
                 || value instanceof Integer 
                 || value instanceof Long 
                 || value instanceof Float 
                 || value instanceof Double 
                 || value instanceof BigDecimal
                 || value instanceof java.util.Date 
                ){
    			 result.put(key, value);
    		 }else if (value instanceof Collection){
    			 List<Object> list= new ArrayList<>();
    			 for (Object item: (List<?>)value){
        			 list.add(beanToMap(item));   				 
    			 }
    			 result.put(key, list);
    		 }else if (value.getClass().getName().startsWith(BASE_PACKAGE)){
    			result.put(key, beanToMap(value)) ;
    		 }else{
    			 throw new RuntimeException("unsupported type: " + value.getClass());
    		 }
    	});
    	return result;
    }
}
