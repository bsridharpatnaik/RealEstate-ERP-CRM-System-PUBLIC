package com.ec.gateway.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.ec.gateway.Model.User;

@Component
public interface UserRepository  extends JpaRepository<User, Long>{

	List<User> findByUserName(String userName);
}
