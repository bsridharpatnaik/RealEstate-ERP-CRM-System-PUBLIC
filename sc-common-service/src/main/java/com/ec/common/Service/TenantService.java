package com.ec.common.Service;

import com.ec.common.Data.TenantInformation;
import com.ec.common.Model.Tenant;
import com.ec.common.Repository.TenantRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TenantService {

    @Autowired
    UserService userService;

    @Value("${spring.profiles.active}")
    private String profile;

    @Autowired
    TenantRepo tenantRepo;

    public List<TenantInformation> fetchTenantList() {
        List<TenantInformation> tenants = new ArrayList<TenantInformation>();
        for (Tenant t : tenantRepo.findAll()) {
            tenants.add(new TenantInformation(t.getTenantLongName(),t.getName(),t.getInventory(),t.getCrm()));
        }
        return tenants;
    }

    public List<String> getValidTenantKeys() {
        List<String> validTenants = new ArrayList<String>();
        for (Tenant t : tenantRepo.findAll()) {
            validTenants.add(t.getName());
        }
        return validTenants;
    }

    public List<TenantInformation> getAllowedTenants() throws Exception {
        List<String> allowedTenantKeys = userService.findTenantsForUser();
        List<TenantInformation> allTenants = fetchTenantList();
        List<TenantInformation> allowedTenants = new ArrayList<TenantInformation>();
        for (TenantInformation ti : allTenants) {
            if (allowedTenantKeys.contains(ti.getTenantCode())) {
                allowedTenants.add(ti);
            }
        }
        return allowedTenants;
    }
}