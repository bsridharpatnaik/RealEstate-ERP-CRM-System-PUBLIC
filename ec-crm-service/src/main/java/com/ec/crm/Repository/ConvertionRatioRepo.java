package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface ConvertionRatioRepo extends BaseRepository<ConversionRatio, String>, JpaSpecificationExecutor<ConversionRatio>
{
	@Query(value="from ConversionRatio where ConversionRatio.ratio = (SELECT max(ratio) from ConversionRatio)")
	List gettopperformer();

}
