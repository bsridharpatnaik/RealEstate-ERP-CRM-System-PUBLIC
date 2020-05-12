package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.AllInventoryTransactions;

@Repository
public interface AllInventoryRepo extends BaseRepository<AllInventoryTransactions, Long>
{

}