package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.DashboardMachineOnRentDAO;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.model.Product;

@Repository
public interface MachineryOnRentRepo extends BaseRepository<MachineryOnRent, Long>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	MachineryOnRent save(MachineryOnRent entity);
	
	@Query(value="SELECT count(*) from MachineryOnRent m where m.machinery.machineryId=:machineryId")
	int machineryUsageCount(@Param("machineryId")Long machineryId);

	//@Query(value="SELECT count(*) from MachineryOnRent m where m.contractor.contractorId=:contractorId")
	//int contractorUsageCount(Long contractorId);
	
	@Query(value="SELECT count(*) from MachineryOnRent m where m.usageLocation.locationId=:locationId")
	int locationUsageCount(@Param("locationId")Long locationId);

	@Query(value="SELECT new com.ec.application.data.DashboardMachineOnRentDAO(date,machinery.machineryName,mode) from MachineryOnRent m")
	List<DashboardMachineOnRentDAO> findForDashboard(Pageable pageable);
	
	@Query(value="SELECT count(*) from MachineryOnRent m where m.supplier.contactId=:id")
	int supplierUsageCount(@Param("id") Long id);
}

