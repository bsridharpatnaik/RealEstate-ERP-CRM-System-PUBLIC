package com.ec.common.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.common.Model.Address;
import com.ec.common.Model.ContactAllInfo;

@Repository
public interface AddressRepo extends JpaRepository<Address, Long>, JpaSpecificationExecutor<ContactAllInfo>
{
	
}
