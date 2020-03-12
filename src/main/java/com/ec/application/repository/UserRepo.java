package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.UserRoles.User;

@Repository
public interface UserRepo extends BaseRepository<User, Long>
{
	@Query(value="SELECT c FROM User c WHERE c.userName like :userName")
    public ArrayList<User> findUserByUsername(@Param("userName") String userName);
}
