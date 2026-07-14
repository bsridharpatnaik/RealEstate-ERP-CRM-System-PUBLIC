package com.ec.application.aspects;

import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.TenantService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantAspect {

    @Autowired
    private TenantService tenantService;

    /**
     * Switches to master schema for the duration of the annotated method,
     * then restores the original tenant. This prevents @UseDefaultTenant
     * from permanently poisoning ThreadLocal for subsequent operations
     * (e.g. activity log writes) that need the project schema.
     *
     * Triggers if:
     *  - method has @UseDefaultTenant
     *  - OR class has @UseDefaultTenant
     */
    @Around("@annotation(UseDefaultTenant) || @within(UseDefaultTenant)")
    public Object applyDefaultTenant(ProceedingJoinPoint pjp) throws Throwable {
        String originalTenant = ThreadLocalStorage.getTenantName();
        tenantService.setDefaultTenant();
        try {
            return pjp.proceed();
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }
    }
}
