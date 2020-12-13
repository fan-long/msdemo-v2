package com.msdemo.v2.common.composite.handler;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.composite.CompositionFactory;

@Component
@ManagedResource(objectName = "compose:name=Management", description = "compose dynamic deployment")
public class ManagementHandler {

	@ManagedAttribute(description = "process list")	
	public String[] getProcessList(){
		return CompositionFactory.nameList();
	}
	
	@ManagedOperation(description = "show process flow definition as xml format")
	@ManagedOperationParameters({ 
		@ManagedOperationParameter(name = "name", description = "process flow name, for example: Trans111222"),
	})
	public String showDefinition(String name){
		if (CompositionFactory.isDeployed(name))
			return CompositionFactory.get(name).toXml();
		else
			return "not found";
	}
	
	@ManagedOperation(description = "re-define process flow by text as xml format")
	@ManagedOperationParameters({ 
		@ManagedOperationParameter(name = "xml", description = "process flow definition xml"),
	})
	public String uploadDefinition(String xml) throws Exception{
		return CompositionFactory.deploy(XmlDefinitionHelper.fromXml(xml),true);
	}
}
