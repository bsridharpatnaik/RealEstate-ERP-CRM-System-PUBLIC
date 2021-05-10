package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ec.crm.Model.Address;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.ReusableClasses.BaseRepository;

public interface StagnantStatsRepo extends BaseRepository<StagnantStats, String>, JpaSpecificationExecutor<StagnantStats>
{

}
