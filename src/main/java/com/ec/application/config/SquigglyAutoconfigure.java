package com.ec.application.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bohnman.squiggly.Squiggly;
import com.github.bohnman.squiggly.web.RequestSquigglyContextProvider;
import com.github.bohnman.squiggly.web.SquigglyRequestFilter;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class SquigglyAutoconfigure {
	@Bean
	public FilterRegistrationBean squigglyRequestFilter(ObjectMapper objectMapper) 
	{
	        Squiggly.init(objectMapper, new RequestSquigglyContextProvider());

	        FilterRegistrationBean<SquigglyRequestFilter> filter = new FilterRegistrationBean<>();
	        filter.setFilter(new SquigglyRequestFilter());
	        filter.setOrder(1);
	        return filter;
	    }

}
