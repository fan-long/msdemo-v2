package com.msdemo.v2.resource.datasource;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.msdemo.v2.common.ManagedThreadLocal;

/**
 * 启用动态数据源配置,可用于读写分离及分库数据源配置
 * 只用于运行时选择指定的一个数据源，不支持多数据源对应的多事务管理器 
 * @author LONGFAN
 *
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.dynamic-routing", havingValue = "true")
@ConfigurationProperties("spring.datasource")
@SuppressWarnings("unchecked")
public class DynamicDataSourceConfiguration {
	private static Map<String, Object> druidConfigMap;

	private static final String DATASOURCE_PREFIX = "datasource-";
	private static final String PROPERTY_DEFAULT="default";
	@Bean
	@Primary
	public DynamicDataSource dataSource() {
		DynamicDataSource ds = new DynamicDataSource();
		ds.setTargetDataSources(new LinkedHashMap<Object, Object>());
		return ds;
	}

	@Bean
	public DynamicDruidDataSource dynamicDruidDataSource() {
		return new DynamicDruidDataSource();
	}

	public Map<String, Object> getDruid() {
		return druidConfigMap;
	}

	public void setDruid(Map<String, Object> druid) {
		DynamicDataSourceConfiguration.druidConfigMap = druid;
	}

	@ConfigurationProperties("spring.datasource.druid")
	static class DynamicDruidDataSource {

		private LinkedHashMap<String, Object> routing;

		@Autowired
		DynamicDataSource dynamicDataSource;

		@Autowired
		ApplicationContext applicationContext;

		public LinkedHashMap<String, Object> getRouting() {
			return routing;
		}

		public void setRouting(LinkedHashMap<String, Object> routing) {
			this.routing = routing;
		}

		/**
		 * 根据DataSource创建bean并注册到容器中
		 * 
		 * @throws SQLException
		 */
		@PostConstruct
		private void populateDataSourceBeans() throws SQLException {
			LinkedHashMap<Object, Object> targetDataSources = new LinkedHashMap<>();
			DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
					.getAutowireCapableBeanFactory();
			for (String key : routing.keySet()) {
				String beanKey = DATASOURCE_PREFIX + key;
				boolean isDefault=false;

				Map<String, Object> props = (Map<String, Object>) routing.get(key);
				if (props.containsKey(PROPERTY_DEFAULT) && 
						Boolean.valueOf(props.get(PROPERTY_DEFAULT).toString()))
					isDefault=true;
				props.remove(PROPERTY_DEFAULT);
				props.putAll(druidConfigMap);
				// 组装bean
				AbstractBeanDefinition beanDefinition = getBeanDefinition(beanKey, props);
				// 注册bean
				beanFactory.registerBeanDefinition(beanKey, beanDefinition);
				DruidDataSource ds = (DruidDataSource) applicationContext.getBean(beanKey);
				targetDataSources.put(key, ds);
				if (isDefault) {
					dynamicDataSource.setDefaultTargetDataSource(ds);
				}
			}
			// 将创建的map对象set到 targetDataSources；
			dynamicDataSource.setTargetDataSources(targetDataSources);
			// 必须执行此操作，才会重新初始化AbstractRoutingDataSource 中的 resolvedDataSources，也只有这样，动态切换才会起效
			dynamicDataSource.afterPropertiesSet();
		}

		/**
		 * TODO: read property: 'spring.datasource.type' to check XA transaction setting
		 * replace with 'com.alibaba.druid.pool.xa.DruidXADataSource' if needed
		 */
		private AbstractBeanDefinition getBeanDefinition(String beanKey, Map<String, Object> props) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
			builder.getBeanDefinition().setAttribute("id", beanKey);			
			builder.setInitMethodName("init");
			builder.setDestroyMethodName("close");
			builder.addPropertyValue("name", beanKey);
			
			// 从模板继承属性,但仅可用于XML方式Bean定义，对于ConfigurationProperties的参数绑定无效
			// builder.setParentName(DATASOURCE_PREFIX+DATASOURCE_DEFAULT);
			for (String prop : props.keySet()){
				if (props.get(prop) instanceof Map) continue;
				builder.addPropertyValue(prop,props.get(prop));
			}
			return builder.getBeanDefinition();
		}
	}

	public static class DynamicDataSource extends AbstractRoutingDataSource {

		private static final ManagedThreadLocal<String> contextHolder = new ManagedThreadLocal<>(
				DynamicDataSource.class.getSimpleName(),String.class,false);

		/**
		 * 是实现数据源切换要扩展的方法， 该方法的返回值就是项目中所要用的DataSource的key值，
		 * 拿到该key后就可以在resolvedDataSource中取出对应的DataSource，
		 * 如果key找不到对应的DataSource就使用默认的数据源。
		 */
		@Override
		protected Object determineCurrentLookupKey() {
			return getDataSource();
		}

		/**
		 * 绑定当前线程数据源路由的key
		 */
		public static void setDataSource(String dataSource) {
			contextHolder.set(dataSource);
		}

		/**
		 * 获取当前线程的数据源路由的key
		 */
		public static String getDataSource() {
			return contextHolder.get();
		}

	}

}
