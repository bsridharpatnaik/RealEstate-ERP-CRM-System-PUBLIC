package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ec.crm.Model.SecurityUser;
import com.ec.crm.ReusableClasses.BaseRepository;

public interface SecurityUserRepo extends BaseRepository<SecurityUser, Long>, JpaSpecificationExecutor<SecurityUser>{

}
