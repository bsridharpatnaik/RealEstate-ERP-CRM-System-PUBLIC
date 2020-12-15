package com.ec.common.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
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
public class JwtUserDetailsService implements UserDetailsService
{

	@Autowired
	private UserRepository userRepo;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
	{
		boolean validateTenant = false;
		if (username.contains("#"))
		{
			validateTenant = true;
		}
		String userName = null;
		String tenant = null;
		if (validateTenant)
		{
			userName = username.substring(0, username.lastIndexOf("#"));
			tenant = username.substring(username.lastIndexOf("#") + 1, username.length());
		} else
		{
			userName = username;
		}

		List<com.ec.common.Model.User> findByUserName = userRepo.findByUserName(userName);
		if (findByUserName.size() > 0)
		{
			com.ec.common.Model.User user = findByUserName.get(0);
			if (validateTenant)
			{
				if (StringUtils.isBlank(user.getTenants()))
				{
					throw new AuthenticationServiceException("User not assigned to any tenant: " + userName);
				}
				if (!user.getTenants().contains(tenant) && !user.getTenants().equals("all"))
				{
					throw new AuthenticationServiceException("User not assigned to any tenant: " + userName);
				}
			}
			List<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());

			List<GrantedAuthority> authorities = roles == null ? Collections.emptyList()
					: roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

			return new User(userName, user.getPassword(), authorities);

		} else
		{
			throw new UsernameNotFoundException("User not found with username: " + userName);
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