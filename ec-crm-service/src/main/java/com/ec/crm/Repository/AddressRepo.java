package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.Address;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface AddressRepo extends BaseRepository<Address, Long>, JpaSpecificationExecutor<Address>
{
	
}
