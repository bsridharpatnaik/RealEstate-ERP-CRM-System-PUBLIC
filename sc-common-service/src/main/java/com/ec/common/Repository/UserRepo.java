package com.ec.common.Repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.common.Model.User;

@Repository
public interface UserRepo extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>
{
	@Query(value = "SELECT c FROM User c WHERE c.userName like :userName")
	public ArrayList<User> findUserByUsername(@Param("userName") String userName);

	@Query(value = "SELECT DISTINCT userName FROM User c")
	public List<String> findUserNames();

	@Query(value = "SELECT id FROM User c WHERE c.userName like :userName")
	public Long findId(@Param("userName") String userName);

	//@Query(value = "SELECT tenants FROM User c WHERE c.userName like :username")
	//public String fetchTenantsForUser(@Param("username") String username);
}
