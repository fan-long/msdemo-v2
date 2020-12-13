package com.msdemo.v2.common.composite.handler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.msdemo.v2.common.composite.CompositionFactory;
import com.msdemo.v2.common.composite.CompositionFactory.DefinitionBuilder;
import com.msdemo.v2.common.composite.ProcessDefinition;
import com.msdemo.v2.common.composite.ProcessDefinition.TxnType;
import com.msdemo.v2.common.composite.chain.AbsProcessInterceptor;
import com.msdemo.v2.common.composite.flow.AbstractFlow;
import com.msdemo.v2.common.composite.flow.AbstractFlow.AbstractFlowBuilder;
import com.msdemo.v2.common.composite.flow.AbstractInvokerFlow;
import com.msdemo.v2.common.composite.flow.AsyncFlow;
import com.msdemo.v2.common.composite.flow.ConditionFlow;
import com.msdemo.v2.common.composite.flow.DynamicTxnFlow;
import com.msdemo.v2.common.composite.flow.GenericFlow;
import com.msdemo.v2.common.composite.flow.ParallelFlow;
import com.msdemo.v2.common.composite.flow.ScriptFlow;
import com.msdemo.v2.common.composite.flow.SimpleFlow;
import com.msdemo.v2.common.composite.flow.VerificationFlow;
import com.msdemo.v2.common.composite.flow.listeners.IFlowListener;
import com.msdemo.v2.common.composite.param.INewInstance;
import com.msdemo.v2.common.composite.param.ParamMapping;
import com.msdemo.v2.common.composite.param.ParamMapping.Type;
import com.msdemo.v2.common.composite.spi.IMappingWrapper;
import com.msdemo.v2.common.composite.spi.IResultWrapper;
import com.msdemo.v2.common.invocation.invoker.InvocationProxy;
import com.msdemo.v2.common.verification.IVerificationRule;
import com.msdemo.v2.common.verification.chain.BaseHandlerBuilder;
import com.msdemo.v2.common.verification.chain.IVerificationHandler;
import com.msdemo.v2.common.verification.chain.IVerificationParam;
import com.msdemo.v2.resource.management.SpringContextHolder;

@SuppressWarnings("unchecked")
public class XmlDefinitionHelper {
	
	private static Logger logger =LoggerFactory.getLogger(XmlDefinitionHelper.class);
	public static final String DELIMITER=",";

	public static ProcessDefinition fromXml(String xml) throws Exception{
		DocumentBuilderFactory bbf = DocumentBuilderFactory.newInstance();
		// 获取构建对象
		DocumentBuilder docBuilder = bbf.newDocumentBuilder();
		
		try (StringReader stringReader=new StringReader(xml)){
	        InputSource is = new InputSource(stringReader);
	        Document doc = docBuilder.parse(is);
			Element root = doc.getDocumentElement();
			DefinitionBuilder builder=CompositionFactory.process(root.getAttribute("name"))
				.transaction(TxnType.valueOf(root.getAttribute("txnType")));			
			String handlerClass=root.getAttribute("exceptionHandler");
			if (StringUtils.isNotEmpty(handlerClass))
				builder.exception((IExceptionHandler)Class.forName(handlerClass).newInstance());
			String invokerClass=root.getAttribute("invoker");
			if (StringUtils.isNotEmpty(invokerClass)){
				builder.invoker((InvocationProxy)SpringContextHolder.getSpringBeanByType(invokerClass));
			}
			NodeList childs= root.getChildNodes();
			for (int i=0;i<childs.getLength();i++){
				Node node=childs.item(i);
				if (node.getNodeName().equals("flowList")){
					for(int k=0;k<node.getChildNodes().getLength();k++){
						AbstractFlow flow =buildFlow(node.getChildNodes().item(k));
						if (flow!=null) builder.next(flow);
					}
				}else if (node.getNodeName().equals("interceptors")){
					for(int k=0;k<node.getChildNodes().getLength();k++){
						if (node.getChildNodes().item(k).getNodeName().equals("interceptor")){
							String className=node.getChildNodes().item(k).getTextContent();							
							String order="0";
							if (node.getChildNodes().item(k).getAttributes().getNamedItem("order")!=null){
								order =node.getChildNodes().item(k).getAttributes().getNamedItem("order").getTextContent();
							}
							AbsProcessInterceptor interceptor=(AbsProcessInterceptor) SpringContextHolder.getSpringBeanByType(className);
							builder.interceptor(interceptor!=null?interceptor:(AbsProcessInterceptor) Class.forName(className).newInstance()
									, Integer.parseInt(order));
						}
					}
				}else if (node.getNodeName().equals("result")){
					String dtoClass="";
					ParamMapping mapping= null;
					if (node.getAttributes().getNamedItem("wrapper")!=null){
						builder.result((IResultWrapper<?>) Class.forName(node.getAttributes().getNamedItem("wrapper")
								.getTextContent()).newInstance());
					}else{
						for (int k=0;k<node.getChildNodes().getLength();k++){
							if (node.getChildNodes().item(k).getNodeName().equals("className"))
								dtoClass=node.getChildNodes().item(k).getTextContent();
							else if(node.getChildNodes().item(k).getNodeName().equals("mappings")){
								mapping=buildMapping(node.getChildNodes().item(k));
							}
						}
						if(mapping!=null){
							if (StringUtils.isNotEmpty(dtoClass))
								builder.result((Class<? extends INewInstance>) Class.forName(dtoClass), mapping);
							else
								builder.resultMap(mapping);
						}
					}
				}
			}
			return builder.register(true);
		}		
	}
	
	private static AbstractFlow buildFlow(Node node) throws Exception{
		logger.trace(node.getNodeName());
		switch (node.getNodeName()){
			case "verificationFlow":
				VerificationFlow.Builder vBuilder = VerificationFlow.builder();
				NodeList vChilds= node.getChildNodes();
				for (int i=0;i<vChilds.getLength();i++){
					Node child=vChilds.item(i);
					fillBasicField(vBuilder,child);
					if (child.getNodeName().equals("handlers")){
						for(int k=0;k<child.getChildNodes().getLength();k++){
							if (child.getChildNodes().item(k).getNodeName().equals("handler")){
								Node hNode=child.getChildNodes().item(k);
								IVerificationHandler<IVerificationRule> handler=(IVerificationHandler<IVerificationRule>)
										Class.forName(hNode.getAttributes().getNamedItem("class").getTextContent()).newInstance();
								BaseHandlerBuilder<?> builder = handler.getBuilder();
								for (int j=0;j<hNode.getChildNodes().getLength();j++){
									Node ruleNode=hNode.getChildNodes().item(j);
									if (ruleNode.getNodeName().equals("rule")){
										String command= hNode.getChildNodes().item(j).getAttributes().getNamedItem("command").getTextContent();
										List<Object> params= new ArrayList<>();
										for (int r=0;r<ruleNode.getChildNodes().getLength();r++){
											if (ruleNode.getChildNodes().item(r).getNodeName().equals("argument")){
												String source=ruleNode.getChildNodes().item(r).getAttributes().getNamedItem("source").getTextContent();
												if (ruleNode.getChildNodes().item(r).getAttributes().getNamedItem("class")!=null){
													String paramClass=ruleNode.getChildNodes().item(r).getAttributes().getNamedItem("class").getTextContent();
													params.add(IVerificationParam.newInstance((Class<IVerificationParam>) Class.forName(paramClass),source));
												}else{
													params.add(source);
												}
											}
										}
										if (params.size()>0)
											builder.rule(command, params.toArray());
										else
											builder.rule(command);
									}
								}
								builder.build();
								vBuilder.handler(handler);
							}
						}
					}else if (child.getNodeName().equals("data")){
						vBuilder.data(child.getAttributes().getNamedItem("source").getTextContent());
					}
				}
				return vBuilder.build();
			case "simpleFlow":
				SimpleFlow.Builder sBuilder=SimpleFlow.builder();
				NodeList sChilds= node.getChildNodes();
				for (int i=0;i<sChilds.getLength();i++){
					Node child=sChilds.item(i);
					fillInvokerField(sBuilder,child);
					if (child.getNodeName().equals("nextFlow")){
						for(int k=0;k<child.getChildNodes().getLength();k++){
							AbstractFlow flow=buildFlow(child.getChildNodes().item(k));
							if (flow!=null) sBuilder.next(flow);
						}
					}
				}
				return sBuilder.build();
			case "scriptFlow":
				ScriptFlow.Builder sfBuilder = ScriptFlow.builder();
				NodeList sfChilds= node.getChildNodes();
				for (int i=0;i<sfChilds.getLength();i++){
					Node child=sfChilds.item(i);
					fillBasicField(sfBuilder,child);
					if(child.getNodeName().equals("content")){
						sfBuilder.content(child.getTextContent());
					}else if (child.getNodeName().equals("service")){
						sfBuilder.service(child.getTextContent());
					}else if (child.getNodeName().equals("shared") 
							&& child.getTextContent().equals("true")){
						sfBuilder.shared();
					}else if (child.getNodeName().equals("transactional") 
							&& child.getTextContent().equals("true")){
						sfBuilder.transactional();
					}
				}
				return sfBuilder.build();
			case "genericFlow":
				GenericFlow.Builder gfBuilder = GenericFlow.builder();
				NodeList gfChilds= node.getChildNodes();
				for (int i=0;i<gfChilds.getLength();i++){
					Node child=gfChilds.item(i);
					fillBasicField(gfBuilder,child);
					if(child.getNodeName().equals("domain")){
						gfBuilder.domain(child.getTextContent());
					}else if (child.getNodeName().equals("endpoint")){
						gfBuilder.endpoint(child.getTextContent());
					}else if (child.getNodeName().equals("mappings")){
						gfBuilder.mapping(buildMapping(child));
					}else if (child.getNodeName().equals("mappingWrapper")){
						if (child.getAttributes().getNamedItem("useScript") != null &&
								child.getAttributes().getNamedItem("useScript").getTextContent().equals("true"))
							gfBuilder.useScript();
						else{
							try {
								gfBuilder.wrapper((IMappingWrapper) Class.forName(
										child.getAttributes().getNamedItem("class").getTextContent()).newInstance());
							} catch (Exception e) {
								throw new RuntimeException("mappingWrapper class is invalid");
							}
						}
					}
				}
				return gfBuilder.build();	
			case "asyncFlow":
				AsyncFlow.AsyncBuilder aBuilder=AsyncFlow.builder();
				NodeList aChilds= node.getChildNodes();
				for (int i=0;i<aChilds.getLength();i++){
					Node child=aChilds.item(i);
					fillInvokerField(aBuilder,child);
					if (child.getNodeName().equals("nextFlow")){
						for(int k=0;k<child.getChildNodes().getLength();k++){
							AbstractFlow flow=buildFlow(child.getChildNodes().item(k));
							if (flow!=null) aBuilder.next(flow);
						}
					}
				}
				return aBuilder.build();
			case "parallelFlow":
				ParallelFlow.Builder pBuilder =ParallelFlow.builder();
				NodeList pChilds= node.getChildNodes();
				for (int i=0;i<pChilds.getLength();i++){
					Node child=pChilds.item(i);
					fillBasicField(pBuilder,child);
					if (child.getNodeName().equals("flowList")){
						for(int k=0;k<child.getChildNodes().getLength();k++){
							AbstractFlow flow=buildFlow(child.getChildNodes().item(k));
							if (flow!=null) pBuilder.addFlow(flow);
						}
					}
				}
				return pBuilder.build();
			case "conditionFlow":	
				ConditionFlow.Builder cBuilder =ConditionFlow.builder();
				NodeList cChilds= node.getChildNodes();
				for (int i=0;i<cChilds.getLength();i++){
					Node child=cChilds.item(i);
					fillBasicField(cBuilder,child);
					if (child.getNodeName().equals("conditions")){
						for(int j=0;j<child.getChildNodes().getLength();j++){
							if(child.getChildNodes().item(j).getNodeName().equals("on")){
								Node onNode=child.getChildNodes().item(j);
								String condEL="";
								AbstractFlow condFlow=null;
								for (int k=0;k<onNode.getChildNodes().getLength();k++){
									if (onNode.getChildNodes().item(k).getNodeName().equals("condEL"))
										condEL=onNode.getChildNodes().item(k).getTextContent();
									else {
										AbstractFlow tempFlow=buildFlow(onNode.getChildNodes().item(k));
										if (tempFlow!=null) condFlow=tempFlow;
									}
								}
								cBuilder.on(condEL, condFlow);
							}else if (child.getChildNodes().item(j).getNodeName().equals("noneMatch")){
								Node onMissNode=child.getChildNodes().item(j);
								AbstractFlow tempFlow=buildFlow(onMissNode.getChildNodes().item(0));
								cBuilder.noneMatch(tempFlow);
							}
						}
					}else if (child.getNodeName().equals("breakOnMatch")){
						cBuilder.breakOnMatch(Boolean.valueOf(child.getTextContent()));
					}
				}
				return cBuilder.build();
			case "dynamicTxnFlow":	
				DynamicTxnFlow.Builder txnBuilder =DynamicTxnFlow.builder();
				NodeList txnChilds= node.getChildNodes();
				for (int i=0;i<txnChilds.getLength();i++){
					Node child=txnChilds.item(i);
					fillBasicField(txnBuilder,child);
					if (child.getNodeName().equals("conditions")){
						for(int j=0;j<child.getChildNodes().getLength();j++){
							if(child.getChildNodes().item(j).getNodeName().equals("on")){
								Node onNode=child.getChildNodes().item(j);
								String condEL="";
								TxnType txnType=null;
								for (int k=0;k<onNode.getChildNodes().getLength();k++){
									if (onNode.getChildNodes().item(k).getNodeName().equals("condEL"))
										condEL=onNode.getChildNodes().item(k).getTextContent();
									else if(onNode.getChildNodes().item(k).getNodeName().equals("txnType")){
										txnType=TxnType.valueOf(onNode.getChildNodes().item(k).getTextContent());
									}
								}
								if (StringUtils.isNotEmpty(condEL) && txnType!=null)
									txnBuilder.on(condEL, txnType);
							}
						}
					}
				}
				return txnBuilder.build();
			default:
				return null;
				//throw new RuntimeException("unknown tag: "+node.getNodeName());
		}
	}
	private static void fillBasicField(AbstractFlowBuilder<?, ?> builder, Node node){
		switch (node.getNodeName()){
			case "name":
				builder.name(node.getTextContent());
				if (node.getAttributes().getNamedItem("merge")!=null)
					builder.mergeName(node.getAttributes().getNamedItem("merge").getTextContent());									
				break;
			case "journal":
				if (StringUtils.equalsIgnoreCase(node.getTextContent(),"true"))
					builder.journal(true);
				else if (StringUtils.equalsIgnoreCase(node.getTextContent(),"false"))
					builder.journal(false);
				//not match, use default per flow setting
				break;
			case "listeners":
				NodeList childs= node.getChildNodes();
				try {
					for (int i=0;i<childs.getLength();i++){
						if (childs.item(i).getNodeName().equals("listener")){
							String listenerClass = childs.item(i).getTextContent();
							IFlowListener listener =(IFlowListener) SpringContextHolder.getSpringBeanByType(listenerClass);
							if (listener!=null)
								builder.addListener(listener);
							else
								builder.addListener((IFlowListener) Class.forName(listenerClass).newInstance());
						}
					}
				}catch (Exception e){
					throw new RuntimeException(e);
				}
				break;
		}
	}
	private static void fillInvokerField(AbstractInvokerFlow.FlowBuilder<?, ?> builder, Node node){
		fillBasicField(builder,node);
		switch (node.getNodeName()){		
			case "beanName":
				builder.beanName(node.getTextContent());
				break;
			case "className":	
				builder.className(node.getTextContent());
				break;
			case "mappings":				
				builder.mapping(buildMapping(node));
				break;
			case "mappingWrapper":
				if (node.getAttributes().getNamedItem("useScript") != null &&
						node.getAttributes().getNamedItem("useScript").getTextContent().equals("true"))
					builder.useScript();
				else{
					try {
						builder.wrapper((IMappingWrapper) Class.forName(
								node.getAttributes().getNamedItem("class").getTextContent()).newInstance());
					} catch (Exception e) {
						throw new RuntimeException("mappingWrapper class is invalid");
					}
				}
				break;
			case "methodName":	
				builder.method(node.getTextContent());
				break;	
			case "invoker":
				builder.invokerClass(node.getTextContent());
				break;
		}
	}
	
	private static ParamMapping buildMapping(Node node){
		ParamMapping mapping= new ParamMapping();
		if (node.getAttributes().getNamedItem("type")!=null)
			mapping.targetType(Type.valueOf(node.getAttributes().getNamedItem("type").getTextContent()));
		for (int i=0;i<node.getChildNodes().getLength();i++){
			if(node.getChildNodes().item(i).getNodeName().equals("mapping")){
				NamedNodeMap map =node.getChildNodes().item(i).getAttributes();
				mapping.add(new MutablePair<>(
						map.getNamedItem("target").getTextContent(),
						map.getNamedItem("source").getTextContent()));
			}
		}
		return mapping;
	}
}
