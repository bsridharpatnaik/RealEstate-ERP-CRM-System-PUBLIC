package com.ec.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.ec.gateway.bean.auth.Data.User;

@Component
public interface UserRepository  extends JpaRepository<User, Long>{

	List<User> findByUserName(String userName);
}
