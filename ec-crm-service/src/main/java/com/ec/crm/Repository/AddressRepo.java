package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.crm.Model.Address;

@Repository
public interface AddressRepo extends BaseRepository<Address, Long>, JpaSpecificationExecutor<Address>
{
	
}
