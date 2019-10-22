package com.msdemo.v2.common.compose.handler;

import java.io.StringReader;

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

import com.msdemo.v2.common.compose.ProcessFlow;
import com.msdemo.v2.common.compose.ProcessFlow.TxnType;
import com.msdemo.v2.common.compose.ProcessFlowFactory;
import com.msdemo.v2.common.compose.ProcessFlowFactory.ProcessFlowBuilder;
import com.msdemo.v2.common.compose.flow.AbstractFlow;
import com.msdemo.v2.common.compose.flow.AsyncFlow;
import com.msdemo.v2.common.compose.flow.ConditionFlow;
import com.msdemo.v2.common.compose.flow.DynamicTxnFlow;
import com.msdemo.v2.common.compose.flow.ParallelFlow;
import com.msdemo.v2.common.compose.flow.SimpleFlow;
import com.msdemo.v2.common.compose.param.ParamMapping;

public class XmlDefinitionHelper {
	
	private static Logger logger =LoggerFactory.getLogger(XmlDefinitionHelper.class);


	public static ProcessFlow formXml(String xml) throws Exception{
		DocumentBuilderFactory bbf = DocumentBuilderFactory.newInstance();
		// 获取构建对象
		DocumentBuilder docBuilder = bbf.newDocumentBuilder();
		
		try (StringReader stringReader=new StringReader(xml)){
	        InputSource is = new InputSource(stringReader);
	        Document doc = docBuilder.parse(is);
			Element root = doc.getDocumentElement();
			ProcessFlowBuilder builder=ProcessFlowFactory.build(root.getAttribute("name"))
				.transaction(TxnType.valueOf(root.getAttribute("txnType")));			
			String handlerClass=root.getAttribute("exceptionHandler");
			if (StringUtils.isNotEmpty(handlerClass))
				builder.exception((IExceptionHandler)Class.forName(handlerClass).newInstance());
			
			NodeList childs= root.getChildNodes();
			for (int i=0;i<childs.getLength();i++){
				Node node=childs.item(i);
				if (node.getNodeName().equals("flowList")){
					for(int k=0;k<node.getChildNodes().getLength();k++){
						AbstractFlow flow =buildFlow(node.getChildNodes().item(k));
						if (flow!=null) builder.next(flow);
					}
				}else if (node.getNodeName().equals("result")){
					String dtoClass="";
					ParamMapping mapping= null;
				
					for (int k=0;k<node.getChildNodes().getLength();k++){
						if (node.getChildNodes().item(k).getNodeName().equals("className"))
							dtoClass=node.getChildNodes().item(k).getTextContent();
						else if(node.getChildNodes().item(k).getNodeName().equals("mappings")){
							mapping=buildMapping(node.getChildNodes().item(k));
						}
					}
					if(mapping!=null){
						if (StringUtils.isNotEmpty(dtoClass))
							builder.result(dtoClass, mapping);
						else
							builder.resultMap(mapping);
					}
					
				}
			}
			return builder.register(true);
		}		
	}
	
	private static AbstractFlow buildFlow(Node node){
		logger.trace(node.getNodeName());
		switch (node.getNodeName()){
			case "simpleFlow":
				SimpleFlow.Builder sBuilder=SimpleFlow.builder();
				NodeList sChilds= node.getChildNodes();
				for (int i=0;i<sChilds.getLength();i++){
					Node child=sChilds.item(i);
					fillCommonField(sBuilder,child);
					if (child.getNodeName().equals("nextFlow")){
						for(int k=0;k<child.getChildNodes().getLength();k++){
							AbstractFlow flow=buildFlow(child.getChildNodes().item(k));
							if (flow!=null) sBuilder.next(flow);
						}
					}
				}
				return sBuilder.build();
			case "asyncFlow":
				AsyncFlow.AsyncBuilder aBuilder=AsyncFlow.asyncBuilder();
				NodeList aChilds= node.getChildNodes();
				for (int i=0;i<aChilds.getLength();i++){
					Node child=aChilds.item(i);
					fillCommonField(aBuilder,child);
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
					fillCommonField(pBuilder,child);
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
					fillCommonField(cBuilder,child);
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
							}
						}
					}else if (child.getNodeName().equals("breakOnMatch")){
						cBuilder.breakOnMatch(Boolean.valueOf(child.getTextContent()));
					}
				}
				return cBuilder.build();
			case "dynamicTxnFlow":	
				DynamicTxnFlow.Builder txnBuilder =DynamicTxnFlow.dynamicTxnBuilder();
				NodeList txnChilds= node.getChildNodes();
				for (int i=0;i<txnChilds.getLength();i++){
					Node child=txnChilds.item(i);
					fillCommonField(txnBuilder,child);
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
	
	private static void fillCommonField(AbstractFlow.FlowBuilder<?, ?> builder, Node node){
		switch (node.getNodeName()){
			case "name":
				builder.name(node.getTextContent());
				break;
			case "mergeName":
				if (StringUtils.isNotEmpty(node.getTextContent()))
					builder.mergeName(node.getTextContent());
				break;
			case "beanName":
				builder.beanName(node.getTextContent());
				break;
			case "className":	
				builder.className(node.getTextContent());
				break;
			case "mappings":				
				builder.mapping(buildMapping(node));
				break;
			case "methodName":	
				builder.method(node.getTextContent());
				break;	
		}
	}
	
	private static ParamMapping buildMapping(Node node){
		ParamMapping mapping= new ParamMapping();
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
