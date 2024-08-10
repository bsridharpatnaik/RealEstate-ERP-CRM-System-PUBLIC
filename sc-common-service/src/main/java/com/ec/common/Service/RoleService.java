package com.ec.common.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.common.Model.Role;
import com.ec.common.Repository.RoleRepo;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    @Autowired
    RoleRepo roleRepo;

    public Page<Role> findAll(Pageable pageable) {
        return roleRepo.findAll(pageable);
    }

    public Role createRole(Role payload) throws Exception {
        if (roleRepo.countByName(payload.getName()) > 0)
            throw new Exception("Role already exists!");
        else
            return roleRepo.save(payload);
    }

    @PostConstruct
    public void init() {
        addDefaultRoles("admin");
        addDefaultRoles("inventory-manager");
        addDefaultRoles("inventory-executive");
        addDefaultRoles("crm-manager");
        addDefaultRoles("crm-executive");
    }

    private void addDefaultRoles(String roleName) {
        Role role = roleRepo.findByName(roleName);
        if (role == null) {
            Role newRole = new Role();
            newRole.setName(roleName);
            roleRepo.save(newRole);
        }

    }
}
