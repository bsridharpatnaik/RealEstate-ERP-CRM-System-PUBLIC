package com.ec.common.Data;

import com.ec.common.Model.UserTenantMapping;

import java.util.List;
import java.util.Set;

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

    public Set<UserTenantMapping> getTenantList() {
        return tenantList;
    }

    public void setTenantList(Set<UserTenantMapping> tenantList) {
        this.tenantList = tenantList;
    }

    public List<String> getAllowedTenants() {
        return allowedTenants;
    }

    public void setAllowedTenants(List<String> allowedTenants) {
        this.allowedTenants = allowedTenants;
    }

    public UserReturnData() {
        // TODO Auto-generated constructor stub
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
