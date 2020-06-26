package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.crm.Model.SecurityUser;

public interface SecurityUserRepo extends BaseRepository<SecurityUser, Long>, JpaSpecificationExecutor<SecurityUser>{

}
