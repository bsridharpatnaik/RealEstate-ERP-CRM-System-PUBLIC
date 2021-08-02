package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InwardOutwardTrend;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface InwardOutwardTrendRepo extends BaseRepository<InwardOutwardTrend, String> {

}
