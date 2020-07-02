package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.crm.Model.Broker;

@Repository
public interface BrokerRepo extends BaseRepository<Broker, Long>, JpaSpecificationExecutor<Broker>{

}
