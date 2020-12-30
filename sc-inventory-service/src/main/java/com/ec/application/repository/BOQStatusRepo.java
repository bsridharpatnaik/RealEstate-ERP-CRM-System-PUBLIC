package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.BOQStatus;

@Repository
public interface BOQStatusRepo extends BaseRepository<BOQStatus, String>
{

}
