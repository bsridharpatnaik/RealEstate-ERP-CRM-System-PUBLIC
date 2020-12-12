package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface ConvertionRatioRepo extends BaseRepository<ConversionRatio, Long>, JpaSpecificationExecutor<ConversionRatio>
{
	@Query(value="select c from ConversionRatio c where c.ratio = (SELECT max(ratio) from ConversionRatio)")
	List gettopperformer();

}
