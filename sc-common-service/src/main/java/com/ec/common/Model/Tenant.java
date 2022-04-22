package com.ec.common.Model;

import org.springframework.lang.NonNull;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "tenant")
public class Tenant {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50, unique = true)
    private String name;

    @Column(name="tenant_long_name")
    private String tenantLongName;

    Boolean isCrm;
    Boolean isInventory;

    public Boolean getCrm() {
        return isCrm;
    }

    public void setCrm(Boolean crm) {
        isCrm = crm;
    }

    public Boolean getInventory() {
        return isInventory;
    }

    public void setInventory(Boolean inventory) {
        isInventory = inventory;
    }

    public String getTenantLongName() {
        return tenantLongName;
    }

    public void setTenantLongName(String tenantLongName) {
        this.tenantLongName = tenantLongName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
