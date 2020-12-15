package com.ec.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.ec.application.multitenant.TenantNameInterceptor;

@SuppressWarnings("deprecation")
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter implements WebMvcConfigurer
{

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer)
	{
		configurer.setUseSuffixPatternMatch(false);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry)
	{
		registry.addInterceptor(new TenantNameInterceptor());
	}

}
