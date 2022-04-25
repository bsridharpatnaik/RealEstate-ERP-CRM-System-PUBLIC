package com.ec.application.data;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UserReturnData {
    String username;
    List<String> roles;
    Long id;
    List<String> allowedTenants;
    Set<UserTenantMapping> tenantList;

    public UserReturnData(Long userId, String userName2, List<String> fetchRolesFromSet, List<String> allowedTenants, Set<UserTenantMapping> tenantList) {
        this.id = userId;
        this.username = userName2;
        this.roles = fetchRolesFromSet;
        this.allowedTenants = allowedTenants;
        this.tenantList = tenantList;
    }

    public UserReturnData() {
        // Empty Constructor
    }
}
