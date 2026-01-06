package com.ec.application.service;

import com.ec.application.data.InstanceList;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public String addPrefixForSuncity(String tenantName) {
        if (profile.contains("sc-") && profile.contains("new")) {
            tenantName = "new" + tenantName;
        }
        if (profile.contains("sc-") && profile.contains("v2")) {
            tenantName = tenantName + "v2";
        }
        return tenantName;
    }

    public String removePrefixForSuncity(String tenantName) {
        if (profile.contains("sc-") && profile.contains("new")) {
            tenantName = tenantName.replace("new", "");
        }
        if (profile.contains("sc-") && profile.contains("v2")) {
            tenantName = tenantName.replace("v2", "");
        }
        return tenantName;
    }
}
