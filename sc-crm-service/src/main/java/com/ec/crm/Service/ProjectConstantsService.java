package com.ec.crm.Service;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Model.ProjectConstantsTable;
import com.ec.crm.Repository.ProjectConstantsRepo;
import com.ec.crm.multitenant.ThreadLocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import com.ec.crm.Enums.ConstantKeysEnum;

@Service
public class ProjectConstantsService {

    @Autowired
    ProjectConstantsRepo projectConstantsRepo;

    @Value("${schemas.list}")
    private String schemasList;

    @Autowired
    UserDetailsService userDetailsService;

    @PostConstruct
    public void init() {
        String[] tenants = schemasList.split(",");
        for (String tenant : tenants) {
            ThreadLocalStorage.setTenantName(tenant);
            addDefaultProjectConstants(ConstantKeysEnum.DUMMY.toString().toString(), 100);
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
}
