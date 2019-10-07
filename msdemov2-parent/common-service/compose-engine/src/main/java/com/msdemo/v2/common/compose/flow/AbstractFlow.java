package com.msdemo.v2.common.compose.flow;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.ProcessFlowFactory;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.util.LogUtils;

public abstract class AbstractFlow {
	
	Logger logger =LoggerFactory.getLogger(this.getClass());
	
	String name;
	String mergeName;
	Invoker invoker =new Invoker();

	String beanName;
	String methodName;
	Class<?> className;
	
	public String getName() {
		return name;
	}

	public Invoker getInvoker() {
		return this.invoker;
	}
	
	public void verify(){
		Object bean= invoker.bean==null?ProcessFlowFactory.getSpringBean(beanName):invoker.bean;
		if(bean!=null){
			if (StringUtils.isEmpty(name)) name=beanName;					
			invoker.bean=bean;
			Method method=BeanUtils.findMethodWithMinimalParameters(
					bean.getClass(), methodName);
			if (method!=null){					
				invoker.method=method;
			}else
				throw new RuntimeException(methodName +" is undefined in Spring bean: "
						+invoker.bean);		
		}else
			throw new RuntimeException(beanName +" is undefined in Spring context");

	}
	
	public void execute(ProcessFlowContext context) throws Exception{
		Object result= invoker.invoke(context);
		context.put(this.name, result);
		if (StringUtils.isNotEmpty(this.mergeName))
			context.put(mergeName, result);
	}
	static class Invoker{

		Object bean;
		Method method;
		ParamMapping mapping;
		PreferAccess access = PreferAccess.LOCAL;
		
		Object invoke(ProcessFlowContext context) throws Exception{
			long start=System.currentTimeMillis();
			int count=method.getParameterCount();
			Object result;
			switch (count){
				case 0:
					result= method.invoke(bean, new Object[0]);
					break;
				case 1:
					if (method.getParameterTypes()[0].isPrimitive() ||
							method.getParameterTypes()[0].equals(String.class)){
						result= method.invoke(bean, new Object[]{ParamMapping.parser.parseExpression(mapping.get(0).getRight())
									.getValue(context)});

					}else{
						Object param=method.getParameterTypes()[0].newInstance();
						for (MutablePair<String,String> pair:mapping){
							ParamMapping.parser.parseExpression(pair.getLeft()).setValue(
									param,ParamMapping.parser.parseExpression(pair.getRight())
									.getValue(context));
						}
						result= method.invoke(bean, new Object[]{param});
					}	
					break;
				default:
					Object[] obj=new Object[count];
					for (MutablePair<String,String> pair:mapping){
						obj[Integer.parseInt(pair.getLeft().replaceFirst("_", ""))]=
								ParamMapping.parser.parseExpression(pair.getRight()).getValue(context);
					}
					result= method.invoke(bean, obj);
			}
			LogUtils.cost(null, start, method.getName());
			return result;
		}
		
	}
	
	public static enum PreferAccess{
		LOCAL,REMOTE;
	}
	
	@SuppressWarnings({"rawtypes","unchecked"})
	public static abstract class FlowBuilder<T extends AbstractFlow, P extends FlowBuilder>{
		public FlowBuilder(){
			this.t=init();
		}
		private T t ;
		abstract T init();
		
		public T getFlow(){
			return t;
		}
		
		public P name(String name){
			t.name=name;
			return (P)this;
		}
		
		public P beanName(String beanName){
			t.beanName=beanName;
			return (P)this;
		}
		public P bean(Object bean){
			t.invoker.bean=bean;
			return (P)this;
		}
		public P method(String methodName){
			t.methodName=methodName;
			return (P)this;				
		}
		public P mapping(ParamMapping mapping){
			t.invoker.mapping=mapping;
			return (P)this;
		}
		public P mergeName(String name){
			t.mergeName=name;
			return (P)this;
		}
		public T build(){
			if (StringUtils.isEmpty(t.name)) 
				t.name=t.invoker.bean.getClass().getSimpleName();
			return t;
		}
	}
	
	@SuppressWarnings({"rawtypes"})
	static class FlowFactory<T extends FlowBuilder>{
		public T get(Class<T> clz) {
			try {
				return clz.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
