package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InwardInventoryStatsForDashboardV2;
import com.ec.application.model.OutwardInventoryStatsForDashboardV2;
import org.springframework.stereotype.Repository;

@Repository
public interface OutwardInventoryStatsForDashboardRepo extends BaseRepository<OutwardInventoryStatsForDashboardV2,String> {

}
