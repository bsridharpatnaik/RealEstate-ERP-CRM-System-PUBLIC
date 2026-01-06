package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InventoryReport;
import com.ec.application.model.InventoryTransfer;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransferRepository extends BaseRepository<InventoryTransfer, Long> {
}
