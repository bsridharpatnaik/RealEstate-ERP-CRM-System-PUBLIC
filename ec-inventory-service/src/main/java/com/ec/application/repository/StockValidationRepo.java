package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.StockValidation;

@Repository
public interface StockValidationRepo extends BaseRepository<StockValidation, String>{

}
