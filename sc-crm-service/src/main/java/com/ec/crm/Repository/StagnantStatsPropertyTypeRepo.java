package com.ec.crm.Repository;

import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Model.StagnantStatsPropertyType;
import com.ec.crm.ReusableClasses.BaseRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StagnantStatsPropertyTypeRepo extends BaseRepository<StagnantStatsPropertyType, String>, JpaSpecificationExecutor<StagnantStatsPropertyType>
{

}
