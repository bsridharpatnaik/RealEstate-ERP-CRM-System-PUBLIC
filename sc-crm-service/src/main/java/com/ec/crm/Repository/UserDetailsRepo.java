package com.ec.crm.Repository;

import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Model.UserDetails;
import com.ec.crm.ReusableClasses.BaseRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDetailsRepo extends BaseRepository<UserDetails, String>, JpaSpecificationExecutor<UserDetails> {
}
