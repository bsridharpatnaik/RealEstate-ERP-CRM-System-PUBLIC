package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.IndentInventoryList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndentInventoryListRepo extends BaseRepository<IndentInventoryList, Long> {
    @Query("Select i from IndentInventoryList i where i.lineItemCode = :lineItemCode" )
    List<IndentInventoryList> findByLineItemCode(@Param("lineItemCode")String lineItemCode);
}
