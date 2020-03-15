package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.UserRoles.Role;

@Repository
public interface RoleRepo  extends BaseRepository<Role, Long>
{

	@Query(value="SELECT r FROM Role r WHERE r.name LIKE :name")
	Role findByName(@Param("name") String name);
	
	@Query(value="SELECT name FROM Role")
	List<String> findRoleNames();
}
