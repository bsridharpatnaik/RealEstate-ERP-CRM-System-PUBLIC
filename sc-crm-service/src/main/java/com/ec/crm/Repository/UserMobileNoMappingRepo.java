package com.ec.crm.Repository;

import com.ec.crm.Model.UserMobileNoMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMobileNoMappingRepo extends JpaRepository<UserMobileNoMapping,Long> {
}
