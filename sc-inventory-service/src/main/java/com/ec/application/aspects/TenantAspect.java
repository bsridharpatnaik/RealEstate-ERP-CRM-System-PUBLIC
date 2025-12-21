package com.ec.application.aspects;

import com.ec.application.service.TenantService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantAspect {

    @Autowired
    private TenantService tenantService;

    /**
     * Triggers if:
     *  - method has @UseDefaultTenant
     *  - OR class has @UseDefaultTenant
     */
    @Before("@annotation(UseDefaultTenant) || @within(UseDefaultTenant)")
    public void applyDefaultTenant() {
        tenantService.setDefaultTenant();
    }
}
