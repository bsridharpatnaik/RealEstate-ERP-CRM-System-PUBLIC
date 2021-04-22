package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.StockInformationFromView;
import org.springframework.stereotype.Repository;

@Repository
public interface StockInformationRepo extends BaseRepository<StockInformationFromView, Long> {
}
