package com.ec.application.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InwardInventory;

@Repository
public interface InwardInventoryRepo extends BaseRepository<InwardInventory, Long>
{
	
}
