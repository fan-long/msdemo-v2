package com.msdemo.v2.common.composite.flow;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.util.Assert;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.common.composite.CompositionContext;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.composite.param.ParamMapping.Type;
import com.msdemo.v2.common.composite.spi.IMappingWrapper;
import com.msdemo.v2.common.invocation.invoker.InvocationModel;
import com.msdemo.v2.common.invocation.invoker.InvocationProxy;
import com.msdemo.v2.resource.management.SpringContextHolder;

public abstract class AbstractInvokerFlow extends AbstractFlow {

	static final String ScriptKeyPattern="%s"+CommonConstants.KEY_SPLITTER+"%s"; 
	
	InvocationProxy invoker;
	String invokerClass;
	InvocationModel model;
	
	ParamMapping mapping;
	boolean useScript;
	IMappingWrapper wrapper;
	
	Object bean;
	String beanName;
	String className;
	String methodName;
	
	// since the invocation proxy would be set after build the flow
	// the model should be rebuild base on runtime invocation proxy
	public void verify(ProcessDefinition definition,boolean refreshFlag) {
		if (bean == null) {
			if (StringUtils.isNotEmpty(beanName)) {
				bean = SpringContextHolder.getSpringBeanByName(beanName);
				if (bean == null)
					throw new RuntimeException(beanName + " is undefined in Spring context");
				className=SpringContextHolder.getSpringBeanClassName(bean);
			} 			
		}else{
			//set className for xml output		
			className=SpringContextHolder.getSpringBeanClassName(bean);
		}		
	
		if (useScript){
			String scriptName=String.format(ScriptKeyPattern, definition.getName(),this.name);
			if (refreshFlag)
				wrapper=(IMappingWrapper) definition.getScriptService().refresh(scriptName, null);
			else
				wrapper= (IMappingWrapper) definition.getScriptService().load(scriptName, null);
		}
		if (this.invoker == null) {
			if (StringUtils.isNotBlank(invokerClass)) {
				this.invoker = Optional
						.ofNullable((InvocationProxy) SpringContextHolder.getSpringBeanByType(invokerClass))
						.orElseGet(() -> {
							try {
								return (InvocationProxy) (Class.forName(invokerClass).newInstance());
							} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
								throw new RuntimeException(invokerClass + " not defined");
							}
						});
			}else
				this.invoker=definition.getInvoker();
		}
		
		this.model = this.invoker.buildModel(bean,className,methodName);
	}

	protected void doExecute(CompositionContext context) throws Exception {
		InvocationProxy realInvoker=this.invoker!=null?this.invoker:context.getInvocationProxy();
		Object[] args=wrapper!=null?wrapper.wrap(context):parseMapping(model,context,mapping);
		super.beforeInvoke(context,args);
		Object result = invoke(realInvoker,model,args);
		context.put(this.name, result);
		if (StringUtils.isNotEmpty(this.mergeName))
			context.put(mergeName, result);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static abstract class FlowBuilder<T extends AbstractInvokerFlow, P extends FlowBuilder>
			extends AbstractFlowBuilder<T, P> {

		public P invoker(InvocationProxy invoker) {
			getFlow().invoker = invoker;
			return (P) this;
		}

		public P invokerClass(String invokerClass) {
			getFlow().invokerClass = invokerClass;
			return (P) this;
		}

		public P className(String className) {
			getFlow().className=className;
			return (P) this;
		}

		public P beanName(String beanName) {
			getFlow().beanName=beanName;
			return (P) this;
		}

		public P bean(Object bean) {
			getFlow().bean=bean;
			return (P) this;
		}

		public P method(String methodName) {
			getFlow().methodName=methodName;
			return (P) this;
		}

		public P mapping(ParamMapping mapping) {
			getFlow().mapping=mapping;
			return (P) this;
		}

		public P wrapper(IMappingWrapper wrapper){
			getFlow().wrapper=wrapper;
			return (P) this;
		}
		
		public P useScript(){
			getFlow().useScript=true;
			return (P) this;
		}
		
		public T build() {
			Assert.isTrue(getFlow().bean != null || getFlow().beanName != null
					|| getFlow().className != null, "one of 'beanName' or 'className' should be provided");
			Assert.notNull(getFlow().methodName, "methodName is required");
			Assert.isTrue(getFlow().mapping!=null || getFlow().wrapper!=null || getFlow().useScript, 
					"one of 'mapping' or 'wrapper' or 'useScript' should be provided");
			Assert.notNull(getFlow().name, "flow name is required");
			return (T) super.build();
		}
	}

	public StringBuilder toXml() {
		StringBuilder sb = super.toXml();
		if (StringUtils.isNotEmpty(beanName))
			sb.append("<beanName>").append(beanName).append("</beanName>");
		else if (StringUtils.isNotEmpty(className)) {
			sb.append("<className>").append(className).append("</className>");
		}
		if (StringUtils.isNotEmpty(methodName))
			sb.append("<methodName>").append(methodName).append("</methodName>");
		if (StringUtils.isNotBlank(this.invokerClass))
			sb.append("<invoker>").append(this.invokerClass).append("</invoker>");
		else if (this.invoker!=null && !this.invoker.getClass().equals(InvocationProxy.class))
			sb.append("<invoker>").append(SpringContextHolder.getSpringBeanClassName(this.invoker))
				.append("</invoker>");
		if (mapping != null) {
			sb.append(mapping.toXml());
		}else{
			sb.append("<mappingWrapper ");	
			if (useScript){
				sb.append("useScript=\"true\"/>");
			}else
				sb.append("class=\"").append(wrapper.getClass().getName()).append("\"/>");
		}
		return sb;
	}

	public InvocationModel getModel(){
		return model;
	}
	public ParamMapping getMapping(){
		return mapping;
	}
	
	static Object invoke(InvocationProxy invoker,InvocationModel model,Object[] args) throws Exception {
		return invoker.invoke(model, args);
	}
	
//	static Object invoke(InvocationProxy invoker,InvocationModel model, CompositionContext context, IMappingWrapper wrapper) {
//		return invoker.invoke(model, wrapper.wrap(context));
//	}
	
	static Object[] parseMapping(InvocationModel model, CompositionContext context, ParamMapping mapping) throws Exception {
		switch (model.getMethod().getParameterCount()) {
		case 0:
			// 方法入口无参数传递，直接调用
			return new Object[0];
		case 1: // 一个入口参数
			if (model.getMethod().getParameterTypes()[0].isPrimitive()
					|| model.getMethod().getParameterTypes()[0].equals(String.class)) {
				// 方法入口参数为简单类型或String
				return new Object[] {
						ParamMapping.parser.parseExpression(mapping.get(0).getRight()).getValue(context)};
			} else {
				// 方法入口参数为自定义对象，逐个Field映射
				Object param = model.getMethod().getParameterTypes()[0].newInstance();
				if (Type.List.equals(mapping.getType())){
					for (int i=0;i<mapping.size();i++){
						((List<?>)param).add(null);
					}				
				}
				for (MutablePair<String, String> pair : mapping) {
					ParamMapping.parser.parseExpression(pair.getLeft()).setValue(param,
							ParamMapping.parser.parseExpression(pair.getRight()).getValue(context));
				}
				return new Object[] { param };
			}
		default: // 多于一个入口参数，按照简单类型直接映射
			Object[] obj = new Object[model.getMethod().getParameterCount()];
			for (MutablePair<String, String> pair : mapping) {
				obj[Integer.parseInt(pair.getLeft().replaceFirst("_", ""))] = ParamMapping.parser
						.parseExpression(pair.getRight()).getValue(context);
			}
			return obj;
		}
	}
	public String getClassName(){return this.className;}
	public String getMethodName(){return this.methodName;}
}
