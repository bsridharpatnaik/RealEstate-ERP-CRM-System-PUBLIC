package com.ec.crm.ReusableClasses;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TenantHelper {
    @Value("${schemas.list}")
    private String schemasList;

    private Gson gson = new Gson();
    @Value("${spring.profiles.active}")

    private String profile;

    public String appendNewForNewSuncity(String tenantName) {
        if (profile.contains("sc-") && (profile.contains("new"))) {
            tenantName = "new" + tenantName;
        }
        if (profile.contains("temp"))
            tenantName = tenantName + "temp";

        return tenantName;
    }
}
