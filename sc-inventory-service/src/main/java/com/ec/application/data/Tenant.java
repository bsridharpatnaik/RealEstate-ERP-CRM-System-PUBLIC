package com.ec.application.data;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class Tenant {
    private static final long serialVersionUID = 1L;
    private String name;
    private String tenantLongName;
    Boolean isCrm;
    Boolean isInventory;
}
