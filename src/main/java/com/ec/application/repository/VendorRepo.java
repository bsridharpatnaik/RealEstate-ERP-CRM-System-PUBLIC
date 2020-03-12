package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.BasicEntities.Vendor;


@Repository
public interface VendorRepo extends BaseRepository<Vendor, Long>
{

	boolean existsByVendorName(String VendorName);

	ArrayList<Vendor> findByVendorName(String VendorName);

	@Query(value="SELECT m from Vendor m where vendorName LIKE %:name%")
	ArrayList<Vendor> findByPartialName(@Param("name") String name);

	@Query(value="SELECT vendorId as id,vendorName as name from Vendor m")
	List<IdNameProjections> findIdAndNames();
}
