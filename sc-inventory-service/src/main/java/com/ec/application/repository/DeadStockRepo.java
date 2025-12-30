package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Category;
import com.ec.application.model.DeadStock;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface DeadStockRepo extends BaseRepository<DeadStock, Long> {
    @Modifying
    @Query("DELETE FROM DeadStock d WHERE d.tenantName = :tenant")
    void deleteByTenantName(@Param("tenant") String tenant);
}
