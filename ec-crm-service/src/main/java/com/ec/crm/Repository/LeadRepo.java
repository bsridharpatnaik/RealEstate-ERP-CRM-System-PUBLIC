package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.Lead;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface LeadRepo extends BaseRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

}
