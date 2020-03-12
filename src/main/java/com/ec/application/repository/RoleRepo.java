package com.ec.application.repository;

import java.util.ArrayList;

import org.postgresql.core.BaseStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.UserRoles.Role;
import com.ec.application.model.UserRoles.User;

@Repository
public interface RoleRepo  extends BaseRepository<Role, Long>
{

	@Query(value="SELECT r FROM Role r WHERE r.name LIKE :name")
	Role findByName(@Param("name") String name);
}
