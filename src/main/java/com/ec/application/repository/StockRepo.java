package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Stock.Stock;

@Repository
public interface StockRepo  extends BaseRepository<Stock, Long>
{

}
