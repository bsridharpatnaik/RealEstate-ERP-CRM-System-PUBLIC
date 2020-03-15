package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.OutwardInventory;

@Repository
public interface OutwardInventoryRepo extends BaseRepository<OutwardInventory, Long>
{

}
