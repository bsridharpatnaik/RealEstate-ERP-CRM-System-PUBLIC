package com.ec.common.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.common.Model.Role;



@Repository
public interface RoleRepo  extends JpaRepository<Role, Long>
{

	@Query(value="SELECT r FROM Role r WHERE r.name LIKE :name")
	Role findByName(@Param("name") String name);
	
	@Query(value="SELECT name FROM Role")
	List<String> findRoleNames();

	@Query(value="SELECT count(*) FROM Role where name=:name")
	int countByName(@Param("name") String name);
}
