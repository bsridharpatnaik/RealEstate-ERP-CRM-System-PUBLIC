package com.ec.common.Repository;

import com.ec.common.Model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepo extends JpaRepository<Tenant, String>, JpaSpecificationExecutor<Tenant> {

}
