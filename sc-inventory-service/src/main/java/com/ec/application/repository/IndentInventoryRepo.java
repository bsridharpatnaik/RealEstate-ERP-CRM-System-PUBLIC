package com.ec.application.repository;


import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.InwardInventory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

@Repository
public interface IndentInventoryRepo extends BaseRepository<IndentInventory, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    InwardInventory save(InwardInventory entity);
}
