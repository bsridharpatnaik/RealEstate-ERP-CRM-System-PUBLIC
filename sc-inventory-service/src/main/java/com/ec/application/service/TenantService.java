package com.ec.application.service;

import com.ec.application.data.InstanceList;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Service
public class TenantService {

    @Value("${master.schema}")
    private String defaultTenant;

    @Autowired
    InstanceService instanceService;

    @Value("${spring.profiles.active}")
    private String profile;

    Logger log = LoggerFactory.getLogger(TenantService.class);

    public void setDefaultTenant() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (instanceService.getInstance().equals(InstanceList.suncity)) {
            ThreadLocalStorage.setTenantName(defaultTenant);
        }
    }

    public String fetchTenantFromHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String tenantId = attrs.getRequest().getHeader("tenant-id");
        if (tenantId == null) {
            return null;
        }
        return tenantId;
    }
}
