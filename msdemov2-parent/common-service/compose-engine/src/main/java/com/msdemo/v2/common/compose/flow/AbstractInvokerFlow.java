package com.msdemo.v2.common.compose.flow;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;

import com.msdemo.v2.common.compose.ProcessFlowContext;
import com.msdemo.v2.common.compose.ProcessFlowFactory;
import com.msdemo.v2.common.compose.param.ParamMapping;
import com.msdemo.v2.common.util.LogUtils;

public abstract class AbstractInvokerFlow extends AbstractFlow{
	Logger logger =LoggerFactory.getLogger(this.getClass());

	Invoker invoker =new Invoker();
	String beanName;
	String methodName;
	String className;
	

	public Invoker getInvoker() {
		return this.invoker;
	}
	
	public void verify(){
		Object bean=invoker.bean;
		if (bean==null){
			if (StringUtils.isNotEmpty(beanName)){
				bean =ProcessFlowFactory.getSpringBeanByName(beanName);
			}else{
				bean= ProcessFlowFactory.getSpringBeanByType(className);
			}
			if(bean ==null)
				throw new RuntimeException(beanName +" is undefined in Spring context");				
		}
		
		if (StringUtils.isEmpty(name)) name=beanName;					
		invoker.bean=bean;
		Method method=BeanUtils.findMethodWithMinimalParameters(
				bean.getClass(), methodName);
		if (method!=null){					
			invoker.method=method;
		}else
			throw new RuntimeException(methodName +" is undefined in Spring bean: "
					+invoker.bean);	
		
		if(invoker.mapping==null)
			throw new RuntimeException(" mapping is required");

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
	public static abstract class FlowBuilder<T extends AbstractInvokerFlow, P extends FlowBuilder>
		extends AbstractFlowBuilder<T,P>{
		
			
		public P className(String className){
			getFlow().className=className;
			return (P)this;
		}
		public P beanName(String beanName){
			getFlow().beanName=beanName;
			return (P)this;
		}
		public P bean(Object bean){
			getFlow().invoker.bean=bean;
			getFlow().className=bean.getClass().getName();
			return (P)this;
		}
		public P method(String methodName){
			getFlow().methodName=methodName;
			return (P)this;				
		}
		public P mapping(ParamMapping mapping){
			getFlow().invoker.mapping=mapping;
			return (P)this;
		}

		public T build(){
			if (StringUtils.isEmpty(getFlow().name)) 
				getFlow().name=getFlow().invoker.bean.getClass().getSimpleName();
			return (T) super.build();
		}
	}
	
	
	
	public StringBuilder toXml(){
		StringBuilder sb = super.toXml();
		if (StringUtils.isNotEmpty(beanName))
			sb.append("<beanName>").append(beanName).append("</beanName>");
		else if(StringUtils.isNotEmpty(className)){
			String clzName=className;
			if (AopUtils.isCglibProxy(invoker.bean)){
				clzName=AopUtils.getTargetClass(invoker.bean).getName();
			}else{				
				try {
					clzName = invoker.bean.getClass().getGenericInterfaces()[0].getTypeName();
				} catch (Exception e) {
					//not CGLIB or JDK-proxy, ignore
					logger.warn(invoker.bean.getClass()+" is not a Cglib or JDK proxy, ignored. message: "+e.getMessage());
				}
			}
			sb.append("<className>").append(clzName).append("</className>");
		}
		if (StringUtils.isNotEmpty(methodName))
			sb.append("<methodName>").append(methodName).append("</methodName>");
		if(invoker.mapping!=null){
			sb.append(invoker.mapping.toXml());
		}
		return sb;
	}
	
}
