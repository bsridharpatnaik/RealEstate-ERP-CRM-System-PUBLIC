package com.ec.application.service;

import com.ec.application.multitenant.ThreadLocalStorage;
import org.apache.tomcat.jni.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import com.ec.application.model.ProjectConstants;
import com.ec.application.repository.ProjectConstantsRepo;
import com.ec.application.config.ConstantKeysEnum;

@Service
public class ProjectConstantsService {

    @Autowired
    ProjectConstantsRepo projectConstantsRepo;

    @Value("${schemas.list}")
    private String schemasList;

    @PostConstruct
    public void init() {
        String[] tenants = schemasList.split(",");
        for (String tenant : tenants) {
            ThreadLocalStorage.setTenantName(tenant);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_ADMIN.toString(), 100);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_MANAGER.toString(), 30);
            addDefaultProjectConstants(ConstantKeysEnum.INVENTORY_ALLOWED_DAYS_EXECUTIVE.toString(), 3);
            ThreadLocalStorage.setTenantName(null);
        }

    }

    private void addDefaultProjectConstants(String key, Integer defaultValue) {
        Optional<ProjectConstants> existingEntry = projectConstantsRepo.findByKey(key);
        if (!existingEntry.isPresent()) {
            ProjectConstants projectConstant = new ProjectConstants();
            projectConstant.setKey(key);
            projectConstant.setValue(defaultValue);
            projectConstantsRepo.save(projectConstant);
        }
    }

    public List<ProjectConstants> getAllProjectConstants() {
        return projectConstantsRepo.findAll();
    }

    public ProjectConstants getSingleProjectConstants(Long id) throws Exception {
        return projectConstantsRepo.findById(id).orElseThrow(() -> new Exception("ProjectConstant not found with ID " + id));
    }

    public ProjectConstants updateConstants(Long id, Integer value) throws Exception {
        ProjectConstants constant = projectConstantsRepo.findById(id).orElseThrow(() -> new Exception("ProjectConstant not found with ID " + id));
        constant.setValue(value);
        return projectConstantsRepo.save(constant);
    }
}
