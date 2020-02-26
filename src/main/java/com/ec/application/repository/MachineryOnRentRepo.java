package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Contractor;
import com.ec.application.model.MachineryOnRent;

@Repository
public interface MachineryOnRentRepo extends BaseRepository<MachineryOnRent, Long>
{

}

