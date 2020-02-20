package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.model.User;

@Repository
public interface UserRepo extends JpaRepository<User, Long>
{
	@Query(value="SELECT c FROM User c WHERE c.userName like :userName")
    public ArrayList<User> findUserByUsername(String userName);

	public List<User> findByUserName(String username);
	
}
