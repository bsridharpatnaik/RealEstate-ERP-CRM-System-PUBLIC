package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.crm.Model.Source;

public interface SourceRepo  extends BaseRepository<Source, Long>, JpaSpecificationExecutor<Source>{

}
