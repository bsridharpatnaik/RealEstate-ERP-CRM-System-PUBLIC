package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.LeadActivity;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface LeadActivityRepo extends BaseRepository<LeadActivity, Long>, JpaSpecificationExecutor<LeadActivity> {
	
	
	
}
