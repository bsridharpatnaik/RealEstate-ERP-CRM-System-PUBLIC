
package com.ec.crm.Repository;

import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.RecentActivityForPipeline;
import com.ec.crm.ReusableClasses.BaseRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentActivityForPipelineRepo extends BaseRepository<RecentActivityForPipeline, Long>, JpaSpecificationExecutor<RecentActivityForPipeline>
{

}
