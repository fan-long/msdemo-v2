package com.msdemo.v2.resource.management.acl;

import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationListener;

import io.undertow.Undertow.Builder;

public class AdditionalPortCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>
	, ApplicationListener<WebServerInitializedEvent>{
 
    @Value("${server.admin.port:0}")
    private int adminPort;
    @Value("${server.admin.connection:256}")
    private int adminConnection;
    @Value("${server.admin.thread:5}")
    private int adminThread;
    @Value("${server.admin.acceptCount:128}")
    private int adminAcceptCount;
    @Value("${server.port}")
    private int port;

    private Map<Integer,Object> connectorMap= new HashMap<>();
    
    private static final String ACCEPTOR_THREAD_NAME_KEYWORD="Acceptor";
    
	@Override
	public void customize(ConfigurableServletWebServerFactory factory) {
		if (factory instanceof TomcatServletWebServerFactory) {
			TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory)factory;
            Connector adminConnector = new Connector("HTTP/1.1");
            adminConnector.setScheme("http");
            adminConnector.setPort(adminPort>0?adminPort:(port+10000));     
            AbstractHttp11Protocol<?> protocol=(AbstractHttp11Protocol<?>) adminConnector.getProtocolHandler();
            protocol.setMaxConnections(adminConnection);
            protocol.setMaxThreads(adminThread);
            protocol.setAcceptCount(adminAcceptCount);
            tomcatFactory.addAdditionalTomcatConnectors(adminConnector);
            connectorMap.put(adminConnector.getPort(),adminConnector);
		}else if (factory instanceof UndertowServletWebServerFactory){
			UndertowServletWebServerFactory undertowFactory = (UndertowServletWebServerFactory)factory;
			undertowFactory.addBuilderCustomizers(new UndertowBuilderCustomizer() {
				@Override
				public void customize(Builder builder) {
		            builder.addHttpListener(adminPort>0?adminPort:(port+10000), "0.0.0.0");	
				}
			});			
		}		
	}
	
	public void startConnector(int port){
		try {
			if (!isPortRunning(port))
				((Connector)connectorMap.get(port)).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void stopConnector(int port){
		try {
			if (isPortRunning(port))
				((Connector)connectorMap.get(port)).stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean isPortRunning(int port){
		Map<Thread, StackTraceElement[]> threads=Thread.getAllStackTraces();
		for (Thread t:threads.keySet()){
			if (t.getName().contains(String.valueOf(port)) && 
					StringUtils.containsIgnoreCase(t.getName(),ACCEPTOR_THREAD_NAME_KEYWORD)){
				return true;
			}
		}
		return false;
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		try {
			TomcatWebServer server=(TomcatWebServer) event.getWebServer();
			Connector defaultConnector=server.getTomcat().getConnector();
			connectorMap.put(defaultConnector.getPort(), defaultConnector);
		} catch (Exception e) {
			// not tomcat, ignore;
		}
	}
}