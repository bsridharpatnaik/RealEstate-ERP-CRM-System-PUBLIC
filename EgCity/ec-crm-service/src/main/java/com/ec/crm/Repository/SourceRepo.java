package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.ec.crm.Model.Source;
import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.ReusableClasses.IdNameProjections;

public interface SourceRepo  extends BaseRepository<Source, Long>, JpaSpecificationExecutor<Source>{
	boolean existsBySourceName(String sourceName);
	
	@Query(value="SELECT sourceId as id,sourceName as name from Source s")
	List<IdNameProjections> findIdAndNames();
	
	@Query(value="SELECT  distinct sourceName as name from Source s")
	List<String> findDistinctNames();
}
