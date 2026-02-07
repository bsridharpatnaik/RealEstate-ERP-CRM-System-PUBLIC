package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.RoleConstants;
import com.ec.application.data.UserReturnData;
import com.ec.application.model.ProjectConstantsTable;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    public ProjectConstantsTable updateConstants(Long id, Integer value) throws Exception {
        ProjectConstantsTable constant = projectConstantsRepo.findById(id).orElseThrow(() -> new Exception("ProjectConstant not found with ID " + id));
        constant.setValue(value);
        return projectConstantsRepo.save(constant);
    }

    public Long getInventoryEditDaysForCurrentUser() throws Exception {
        UserReturnData userReturnData = userDetailsService.getCurrentUser();
        for (String role : userReturnData.getRoles()) {
            if (role.toLowerCase().contains(RoleConstants.ADMIN))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_ADMIN.toString()).get().getValue());
            else if (role.toLowerCase().contains(RoleConstants.INVENTORY_MANAGER))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_MANAGER.toString()).get().getValue());
            else if (role.toLowerCase().contains(RoleConstants.INVENTORY_EXECUTIVE))
                return Long.valueOf(projectConstantsRepo.findByKey(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_EXECUTIVE.toString()).get().getValue());;
        }
        return (long) 3;
    }
}
