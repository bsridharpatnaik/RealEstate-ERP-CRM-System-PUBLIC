package com.ec.application.repository;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Vendor;
import com.ec.application.model.Vendor;


@Repository
public interface VendorRepo extends BaseRepository<Vendor, Long>
{

	boolean existsByVendorName(String VendorName);

	ArrayList<Vendor> findByVendorName(String VendorName);

	@Query(value="SELECT m from Vendor m where vendorName LIKE %:name%")
	ArrayList<Vendor> findByPartialName(@Param("name") String name);

	@Query(value="SELECT vendorId,vendorName from Vendor m")
	ArrayList<?> findIdAndNames();
}
