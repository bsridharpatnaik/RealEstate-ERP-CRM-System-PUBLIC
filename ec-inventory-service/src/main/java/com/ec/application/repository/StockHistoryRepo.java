package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.Product;
import com.ec.application.model.StockHistory;

@Repository
public interface StockHistoryRepo extends BaseRepository<StockHistory, Long>
{

}
