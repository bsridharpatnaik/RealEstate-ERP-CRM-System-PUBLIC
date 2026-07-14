package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.RoleConstants;
import com.ec.application.data.UserReturnData;
import com.ec.application.model.ProjectConstantsTable;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import com.ec.application.repository.ProjectConstantsRepo;
import com.ec.application.config.ConstantKeysEnum;

@Service
public class ProjectConstantsService {

    @Autowired
    ProjectConstantsRepo projectConstantsRepo;

    @Autowired
    private SchemaConfig schemaConfig;

    @Autowired
    UserDetailsService userDetailsService;

    @PostConstruct
    public void init() {
        List<String> tenants = schemaConfig.getSchemaList();
        for (String tenant : tenants) {
            ThreadLocalStorage.setTenantName(tenant);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_ADMIN.toString(), 30);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_MANAGER.toString(), 30);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_EXECUTIVE.toString(), 7);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_REJECT_RETURN_DAYS_ADMIN.toString(), 90);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_REJECT_RETURN_DAYS_MANAGER.toString(), 90);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_REJECT_RETURN_DAYS_EXECUTIVE.toString(), 90);
            addDefaultProjectConstants(ConstantKeysEnum.BOQ_BLOCK_ON_EXCEED.toString(), 0);
            addDefaultProjectConstants(ConstantKeysEnum.BOQ_BLOCK_WHEN_MISSING.toString(), 0);
            addDefaultProjectConstants(ConstantKeysEnum.NEAR_EXPIRY_DAYS.toString(), 30);
            ThreadLocalStorage.setTenantName(null);
        }

    }

    private void addDefaultProjectConstants(String key, Integer defaultValue) {
        Optional<ProjectConstantsTable> existingEntry = projectConstantsRepo.findByKey(key);
        if (!existingEntry.isPresent()) {
            ProjectConstantsTable projectConstant = new ProjectConstantsTable();
            projectConstant.setKey(key);
            projectConstant.setValue(defaultValue);
            projectConstantsRepo.save(projectConstant);
        }
    }

    public List<ProjectConstantsTable> getAllProjectConstants() {
        return projectConstantsRepo.findAll();
    }

    public ProjectConstantsTable getSingleProjectConstants(Long id) throws Exception {
        return projectConstantsRepo.findById(id).orElseThrow(() -> new Exception("ProjectConstant not found with ID " + id));
    }

    @CacheEvict(value = "projectConstants", allEntries = true)
    public ProjectConstantsTable updateConstants(Long id, Integer value) throws Exception {
        ProjectConstantsTable constant = projectConstantsRepo.findById(id).orElseThrow(() -> new Exception("ProjectConstant not found with ID " + id));
        constant.setValue(value);
        return projectConstantsRepo.save(constant);
    }

    /**
     * Reads an integer constant for the CURRENT tenant (ThreadLocal). Falls back to the
     * default when the row is missing (e.g. tenant added after startup) or unreadable —
     * config lookups must never break a business operation.
     */
    private int getIntOrDefault(ConstantKeysEnum key, int defaultValue) {
        try {
            Optional<ProjectConstantsTable> entry = projectConstantsRepo.findByKey(key.toString());
            if (entry.isPresent() && entry.get().getValue() != null)
                return entry.get().getValue();
        } catch (Exception e) {
            // fall through to default
        }
        return defaultValue;
    }

    // The getters below are @Cacheable and MUST be called from other beans (Spring
    // proxy). Calling them from inside this class would bypass the cache silently.

    /** Block outward save when requested qty exceeds remaining BOQ (per tenant, 0/1). */
    @Cacheable(value = "projectConstants",
            key = "T(com.ec.application.multitenant.ThreadLocalStorage).getTenantName() + ':boqExceed'")
    public boolean isBoqBlockOnExceed() {
        return getIntOrDefault(ConstantKeysEnum.BOQ_BLOCK_ON_EXCEED, 0) == 1;
    }

    /** Block outward save when no BOQ record exists for product+location (per tenant, 0/1). */
    @Cacheable(value = "projectConstants",
            key = "T(com.ec.application.multitenant.ThreadLocalStorage).getTenantName() + ':boqMissing'")
    public boolean isBoqBlockWhenMissing() {
        return getIntOrDefault(ConstantKeysEnum.BOQ_BLOCK_WHEN_MISSING, 0) == 1;
    }

    /** Near-expiry window in days for stock tiles, expiry alerts and the daily email. */
    @Cacheable(value = "projectConstants",
            key = "T(com.ec.application.multitenant.ThreadLocalStorage).getTenantName() + ':nearExpiry'")
    public int getNearExpiryDays() {
        int days = getIntOrDefault(ConstantKeysEnum.NEAR_EXPIRY_DAYS, 30);
        return days > 0 ? days : 30;
    }

    public Long getRejectReturnDaysForCurrentUser() throws Exception {
        UserReturnData userReturnData = userDetailsService.getCurrentUser();
        for (String role : userReturnData.getRoles()) {
            if (role.toLowerCase().contains(RoleConstants.ADMIN))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_REJECT_RETURN_DAYS_ADMIN.toString()).get().getValue());
            else if (role.equalsIgnoreCase(RoleConstants.PURCHASE_MANAGER))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_REJECT_RETURN_DAYS_MANAGER.toString()).get().getValue());
            else if (role.equalsIgnoreCase(RoleConstants.STORE_INCHARGE) || role.equalsIgnoreCase(RoleConstants.PROJECT_MANAGER) || role.equalsIgnoreCase(RoleConstants.MANAGEMENT))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_REJECT_RETURN_DAYS_EXECUTIVE.toString()).get().getValue());
        }
        return (long) 3;
    }

    public Long getInventoryEditDaysForCurrentUser() throws Exception {
        UserReturnData userReturnData = userDetailsService.getCurrentUser();
        for (String role : userReturnData.getRoles()) {
            if (role.toLowerCase().contains(RoleConstants.ADMIN))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_ADMIN.toString()).get().getValue());
            else if (role.equalsIgnoreCase(RoleConstants.PURCHASE_MANAGER))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_MANAGER.toString()).get().getValue());
            else if (role.equalsIgnoreCase(RoleConstants.STORE_INCHARGE) || role.equalsIgnoreCase(RoleConstants.PROJECT_MANAGER) || role.equalsIgnoreCase(RoleConstants.MANAGEMENT))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_EXECUTIVE.toString()).get().getValue());
        }
        return (long) 3;
    }
}
