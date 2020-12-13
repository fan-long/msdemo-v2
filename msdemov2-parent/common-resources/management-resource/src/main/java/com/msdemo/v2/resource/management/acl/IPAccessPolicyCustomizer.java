package com.msdemo.v2.resource.management.acl;

import org.apache.catalina.valves.RemoteAddrValve;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

import io.undertow.Undertow.Builder;
import io.undertow.server.handlers.IPAddressAccessControlHandler;

/**
 * reference: https://stackoverflow.com/questions/43571505/how-to-find-the-interface-embeddedservletcontainercustomizer-in-spring-boot-2-0
 * @author LONGFAN
 *
 */

public class IPAccessPolicyCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

	//TODO: load allow/deny list from configuration center and enable dynamic refresh
	
	public void customize(ConfigurableServletWebServerFactory factory) {
		if (factory instanceof TomcatServletWebServerFactory) {
			RemoteAddrValve valve = new RemoteAddrValve();
			valve.setDeny("172.24.82.82");
			((TomcatServletWebServerFactory) factory).addContextValves(valve);
		}else if (factory instanceof UndertowServletWebServerFactory){
			UndertowServletWebServerFactory undertowFactory = (UndertowServletWebServerFactory)factory;
			undertowFactory.addBuilderCustomizers(new UndertowBuilderCustomizer() {
				@Override
				public void customize(Builder builder) {
					IPAddressAccessControlHandler handler = new IPAddressAccessControlHandler();
					handler.addDeny("172.24.82.82");
					//TODO: build undertow handler chain
					builder.setHandler(handler);
				}
			});			
		}		
	}

}
