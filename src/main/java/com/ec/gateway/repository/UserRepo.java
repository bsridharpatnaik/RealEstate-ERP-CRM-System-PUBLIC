package com.ec.gateway.repository;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.gateway.bean.auth.Data.User;


@Repository
public interface UserRepo extends JpaRepository<User, Long>
{
	@Query(value="SELECT c FROM User c WHERE c.userName like :userName")
    public ArrayList<User> findUserByUsername(@Param("userName") String userName);
}
