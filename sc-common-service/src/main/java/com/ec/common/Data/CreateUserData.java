package com.ec.common.Data;

import java.util.ArrayList;

public class CreateUserData {
    String username;
    String password;
    ArrayList<String> roles;
    ArrayList<TenantUserDTO> tenants;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<String> roles) {
        this.roles = roles;
    }

    public ArrayList<TenantUserDTO> getTenants() {
        return tenants;
    }

    public void setTenants(ArrayList<TenantUserDTO> tenants) {
        this.tenants = tenants;
    }
}
