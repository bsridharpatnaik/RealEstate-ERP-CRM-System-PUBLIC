package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ec.crm.Model.Address;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.ReusableClasses.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StagnantStatsRepo extends BaseRepository<StagnantStats, String>, JpaSpecificationExecutor<StagnantStats>
{

}
