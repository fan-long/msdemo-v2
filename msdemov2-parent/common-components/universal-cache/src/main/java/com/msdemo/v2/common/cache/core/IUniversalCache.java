package com.msdemo.v2.common.cache.core;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;

import com.msdemo.v2.common.core.INameAwareBean;

/**
 * how to use parameter table cache component: 
 * 1. mapper/service bean MUST implements this interface 
 * 2. query method name in mapper/service bean MUST leading with 'selectBy',
 *    and @CachedQuery(query parameter fields) 
 * 3. selectAll should set fetchSize parameter like '<select id="selectAll" fetchSize="200" ...>'
 * 
 * @author LONGFAN
 *
 * @param <T>
 * 
 * TODO: to support selectByExample(Entity entity)
 */
public interface IUniversalCache<T> extends INameAwareBean {

	List<T> selectAll();
	
	@Override
	default String name() {
		if (AopUtils.isCglibProxy(this)) {
			try {
				Field h = this.getClass().getDeclaredField("CGLIB$CALLBACK_0");
				h.setAccessible(true);
				Object dynamicAdvisedInterceptor = h.get(this);

				Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
				advised.setAccessible(true);

				Object target = ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();

				String cacheKey = target.getClass().getSimpleName();
				return cacheKey;
			} catch (Exception e) {
				throw new RuntimeException("unsupport CGLIB class: " + this, e);
			}
		}else {
			try{
				String fullName = this.getClass().getGenericInterfaces()[0].getTypeName();
				String cacheKey = StringUtils.substringAfterLast(fullName, ".");
				return cacheKey;
			} catch (Exception e) {
				return this.getClass().getSimpleName();
			}
		}
	}
}
