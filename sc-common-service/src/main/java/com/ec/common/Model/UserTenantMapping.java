package com.ec.common.Model;

import com.ec.common.Data.AuthorizationEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

@Entity
@Table(name = "tenant_authorization_mapping")
public class UserTenantMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "mapping_id")
    Long mappingId;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "tenant_name")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    Tenant tenant;

    @Enumerated(EnumType.STRING)
    AuthorizationEnum authorization;



    public Long getMappingId() {
        return mappingId;
    }

    public void setMappingId(Long mappingId) {
        this.mappingId = mappingId;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public AuthorizationEnum getAuthorization() {
        return authorization;
    }

    public void setAuthorization(AuthorizationEnum authorization) {
        this.authorization = authorization;
    }
}
