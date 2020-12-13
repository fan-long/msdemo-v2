package com.msdemo.v2.resource.management;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringContextHolder {

	private static final Logger logger =LoggerFactory.getLogger(SpringContextHolder.class);
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Autowired
	ApplicationContext applicationContext;
	
	private static ApplicationContext context;
	private static String application;
	
	@PostConstruct
	public void init(){
		context= applicationContext;
		application=applicationName;
	}
	
	public static String getApplicationName(){
		return application;
	}
	public static ApplicationContext getContext(){
		return context;
	}
	public static Object getSpringBeanByName(String beanName){
		return context.getBean(beanName);
	}
	public static Object getSpringBeanByType(String className){
		try {
			Class<?> type=Class.forName(className);
			String[] names=context.getBeanNamesForType(type);
			if (names.length>1) 
				throw new RuntimeException("multi bean found for class: "+className);
			else if(names.length==0)
				throw new RuntimeException("bean not found for class: "+className);
			return context.getBean(names[0]);
		} catch (BeansException | ClassNotFoundException e) {
			throw new RuntimeException("bean not defined for class: "+className);
		}
	}
	public static String getSpringBeanClassName(Object bean){
		String className=null;
		if (AopUtils.isCglibProxy(bean)) {
			className = AopUtils.getTargetClass(bean).getName();
		} else {
			try {
				className = bean.getClass().getGenericInterfaces()[0].getTypeName();
			} catch (Exception e) {
				// not CGLIB or JDK-proxy, ignore
				className=bean.getClass().getName();
				logger.warn(bean.getClass() + " is not a Cglib or JDK proxy, ignored. message: "
						+ e.getMessage());
			}
		}
		return className;
	}
}
