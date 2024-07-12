package com.ec.crm.Config;

import com.ec.crm.Service.DashboardServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

@Configuration
public class EmailTemplateConfig {

	Logger log = LoggerFactory.getLogger(DashboardServiceV2.class);

	@Primary
	@Bean
	public FreeMarkerConfigurationFactoryBean factoryBean() {
		FreeMarkerConfigurationFactoryBean bean = new FreeMarkerConfigurationFactoryBean();
		log.info("Setting FreeMarker template loader path to: {}", "classpath:/templates/");
		bean.setTemplateLoaderPath("classpath:/templates");
		return bean;
	}
}
