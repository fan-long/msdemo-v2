package com.msdemo.v2.resource.management.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msdemo.v2.common.CommonConstants;
import com.msdemo.v2.resource.management.ManagementConfiguration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@ConditionalOnProperty(value=ManagementConfiguration.MANANGEMENT_PREFIX+".swagger", havingValue="true")
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

	@Value("${spring.application.name}")
	private String serviceName;

	@Bean
	public Docket createRestApi() {
		return new Docket(DocumentationType.SWAGGER_2)
				.apiInfo(new ApiInfoBuilder().title(serviceName)// API 标题
						.description("RESTful APIs")// API描述
						.contact(new Contact("Administartor", "http://localhost", "admin@local"))// 联系人
						.version("1.0")// 版本号
						.build())
				.select().apis(RequestHandlerSelectors.basePackage(CommonConstants.BASE_PACKAGE))
				.paths(PathSelectors.any()).build();
	}
}
