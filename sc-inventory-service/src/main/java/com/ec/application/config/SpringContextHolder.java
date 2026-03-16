package com.ec.application.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Holds a static reference to the Spring ApplicationContext.
 * Used by JPA entity listeners which are instantiated by JPA (not Spring)
 * and therefore cannot use @Autowired directly.
 */

@Component("springContextHolder")
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            return null; // called before Spring context is ready
        }
        return context.getBean(beanClass);
    }
}