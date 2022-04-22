package com.ec.common.Data;

public class TenantUserDTO {
    String tenantName;
    AuthorizationEnum authorization;

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public AuthorizationEnum getAuthorization() {
        return authorization;
    }

    public void setAuthorization(AuthorizationEnum authorization) {
        this.authorization = authorization;
    }
}
