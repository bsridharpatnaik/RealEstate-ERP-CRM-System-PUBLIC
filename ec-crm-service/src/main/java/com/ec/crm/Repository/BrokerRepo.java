package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.Broker;
import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.ReusableClasses.IdNameProjections;

@Repository
public interface BrokerRepo extends BaseRepository<Broker, Long>, JpaSpecificationExecutor<Broker>{
	boolean existsByBrokerPhoneno(String phone);
	
	@Query(value="SELECT brokerId as id,brokerName as name from Broker b")
	List<IdNameProjections> findIdAndNames();
}
