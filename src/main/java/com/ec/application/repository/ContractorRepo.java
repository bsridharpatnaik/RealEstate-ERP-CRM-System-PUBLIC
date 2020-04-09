package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Contractor;

@Repository
public interface ContractorRepo extends BaseRepository<Contractor, Long>
{
	@Query(value="SELECT contactId as id,name as name from Contractor m")
	List<IdNameProjections> getContractorNames();

}