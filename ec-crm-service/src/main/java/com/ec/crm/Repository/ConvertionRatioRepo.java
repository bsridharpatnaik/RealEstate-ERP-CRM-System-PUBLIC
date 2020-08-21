package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface ConvertionRatioRepo extends BaseRepository<ConversionRatio, String>, JpaSpecificationExecutor<ConversionRatio>
{

}
