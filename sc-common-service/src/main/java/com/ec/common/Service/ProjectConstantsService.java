package com.ec.common.Service;

import com.ec.common.Configuration.ConstantKeys;
import com.ec.common.Model.ProjectConstants;
import com.ec.common.Repository.ProjectConstantsRepo;
import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectConstantsService {

    @Autowired
    ProjectConstantsRepo projectConstantsRepo;

    @PostConstruct
    public void init() {
        // Define the keys and default values
        addDefaultProjectConstants(ConstantKeys.INVENTORY_ALLOWED_DAYS_ADMIN.toString(), 100);
        addDefaultProjectConstants(ConstantKeys.INVENTORY_ALLOWED_DAYS_MANAGER.toString(), 30);
        addDefaultProjectConstants(ConstantKeys.INVENTORY_ALLOWED_DAYS_EXECUTIVE.toString(), 3);
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

    public List<ProjectConstants> getAllProjectConstants(){
        return projectConstantsRepo.findAll();
    }

    public ProjectConstants getSingleProjectConstants(Long id) throws Exception {
        return projectConstantsRepo.findById(id).orElseThrow(() -> new Exception("ProjectConstant not found with ID " + id));
    }

    public  ProjectConstants updateConstants(Long id, Integer value) throws Exception {
        ProjectConstants constant = projectConstantsRepo.findById(id).orElseThrow(() -> new Exception("ProjectConstant not found with ID " + id));
        constant.setValue(value);
        return projectConstantsRepo.save(constant);
    }
}
