package com.msdemo.v2.common.verification;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class VerificationChain { 

	List<IVerificationHandler<?>> handlerList = new ArrayList<>();

	Set<Class<? extends IVerificationAware>> awareList = new HashSet<>();
	
	public static Builder builder(){
		return new Builder();
	}
	public static class Builder {
		private VerificationChain instance = new VerificationChain();

		public Builder handle(IVerificationHandler<?> handler) {
			instance.handlerList.add(handler);
			return this;
		}

		@SuppressWarnings("unchecked")
		public VerificationChain build() {
			
			/* for technical research only, input/output validation*/
			instance.handlerList.forEach( (h) -> {
				BaseHandlerBuilder.IRuleFactory ruleFactory= h.ruleFactory();
				for (Method m:ruleFactory.getClass().getMethods()){
					if (Modifier.isPublic(m.getModifiers()) && 
							m.getGenericReturnType() instanceof ParameterizedType)
						for (Type type:((ParameterizedType)m.getGenericReturnType()).getActualTypeArguments()){
							try{
								instance.awareList.add(((Class<? extends IVerificationAware>)type)); //.getInterfaces()[0]
							}catch(Exception e){
								//ignore
							}
						}					
				}
			});
			
			return instance;
		}
	}
	public void verify(IVerificationAware req) {
		verify(req,null);
	}
	public void verify(IVerificationAware req,Object paramContext) {
		/* for technical research only, input/output validation*/
		Set<Class<?>> reqInterface = new HashSet<>();
		for (Class<?> itf: req.getClass().getInterfaces())
			reqInterface.add(itf);
		Set<Class<?>> notFound = new HashSet<>();
		for (Class<?> itf: this.awareList){
			if (!(reqInterface.contains(itf)))
				notFound.add(itf);
		}
		if (!(notFound.isEmpty())){
			throw new VerificationException(VerificationException.HANDLER_INSTANCE_ERROR,
					StringUtils.join(notFound.toArray()));
		}
				
		for (IVerificationHandler<?> handler : handlerList) {
			handler.handle(req,paramContext);
		}
	}
	
	public List<IVerificationHandler<?>> getHandler(){
		return this.handlerList;
	}
}
