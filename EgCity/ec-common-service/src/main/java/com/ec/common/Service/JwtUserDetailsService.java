package com.ec.common.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ec.common.Model.Role;
import com.ec.common.Repository.UserRepository;


@Service
public class JwtUserDetailsService implements UserDetailsService {
	

	@Autowired
	private UserRepository userRepo;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		List<com.ec.common.Model.User> findByUserName = userRepo.findByUserName(username);
		if(findByUserName.size() > 0) {
			com.ec.common.Model.User user = findByUserName.get(0);
			
			List<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());

			List<GrantedAuthority> authorities = roles == null ? Collections.emptyList()
					: roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

			return new User(username, user.getPassword(), authorities);

		}else {
			throw new UsernameNotFoundException("User not found with username: " + username);
		}
	}
	public String fetchRoles()
	{
		/*
		 * Collection<? extends GrantedAuthority> authorities = String isAdmin =
		 * "nonadmin";
		 * 
		 * for(GrantedAuthority grantedAuthority : authorities) {
		 * System.out.println("{{{"+grantedAuthority.toString());
		 * if(grantedAuthority.getAuthority().equals("admin")) {
		 * System.out.println(grantedAuthority.getAuthority()); isAdmin = "admin";
		 * //break; } } return isAdmin;
		 */
		return "";
		}
	
}