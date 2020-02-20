package com.ec.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.model.Role;
import com.ec.application.model.User;

@Repository
public interface RoleRepo  extends JpaRepository<Role, Long>
{

	@Query(value="SELECT r FROM Role r WHERE r.name LIKE :name")
	Role findByName(String name);

}
