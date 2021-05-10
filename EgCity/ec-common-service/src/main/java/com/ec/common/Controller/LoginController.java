package com.ec.common.Controller;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Data.JwtResponse;
import com.ec.common.Data.LoginData;
import com.ec.common.Data.UserSignInData;
import com.ec.common.JWTUtils.JWTTokenUtils;
import com.ec.common.Repository.UserRepo;
import com.ec.common.Service.JwtUserDetailsService;

@RestController
public class LoginController
{

	public static String role = "";
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JWTTokenUtils jwtTokenUtil;

	@Autowired
	UserRepo uRepo;

	@Autowired
	private JwtUserDetailsService userDetailsService;

	@PostMapping(value = "/ec/login", produces =
	{ "application/json", "text/json" })
	public ResponseEntity<?> login(@RequestBody UserSignInData userData) throws Exception
	{
		LoginData loginData = new LoginData();
		String username = userData.getUserName().trim();
		String password = userData.getPassword();
		authenticate(username, password);
		final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		final String token = jwtTokenUtil.generateToken(userDetails);

		Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
		ArrayList<String> roles = new ArrayList<String>();
		for (GrantedAuthority grantedAuthority : authorities)
		{
			roles.add(grantedAuthority.getAuthority());
		}
		String name = username;
		Long userid = uRepo.findId(name.trim());
		return ResponseEntity.ok(new JwtResponse(name, token, userid, roles));
	}

	private void authenticate(String username, String password) throws Exception
	{
		try
		{
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (DisabledException e)
		{
			throw new Exception("USER_DISABLED", e);
		} catch (BadCredentialsException e)
		{
			throw new Exception("INVALID_CREDENTIALS", e);
		}
	}
}
