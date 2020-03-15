package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.BasicEntities.Contractor;
import com.ec.application.model.BasicEntities.Machinery;

@Repository
public interface ContractorRepo extends BaseRepository<Contractor, Long>
{

	boolean existsByContractorName(String contractorName);

	ArrayList<Contractor> findBycontractorName(String contractorName);

	@Query(value="SELECT m from Contractor m where contractorName LIKE %:name%")
	ArrayList<Contractor> findByPartialName(@Param("name") String name);

	@Query(value="SELECT contractorId as id,contractorName as name from Contractor m")
	List<IdNameProjections> findIdAndNames();
}
