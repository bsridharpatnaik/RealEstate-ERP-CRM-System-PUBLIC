package com.ec.application.repository;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Contractor;

@Repository
public interface ContractorRepo extends BaseRepository<Contractor, Long>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Contractor save(Contractor entity);

	@Query(value = "SELECT contactId as id,name as name from Contractor m  order by name")
	List<IdNameProjections> findIdAndNames();

}