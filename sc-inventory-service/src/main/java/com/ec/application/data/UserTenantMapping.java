package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;

@Data
@AllArgsConstructor
public class UserTenantMapping {
    Long mappingId;
    Tenant tenant;
    @Enumerated(EnumType.STRING)
    AuthorizationEnum authorization;
}
