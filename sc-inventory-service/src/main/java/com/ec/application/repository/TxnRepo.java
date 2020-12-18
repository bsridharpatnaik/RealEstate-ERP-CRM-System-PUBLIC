package com.ec.application.repository;

import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.TxnSchema;

@Repository
public interface TxnRepo extends BaseRepository<TxnSchema, Long>
{
	
}
