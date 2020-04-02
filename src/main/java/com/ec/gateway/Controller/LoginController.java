package com.ec.gateway.Controller;

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

import com.ec.gateway.Data.JwtResponse;
import com.ec.gateway.Data.LoginData;
import com.ec.gateway.Data.UserSignInData;
import com.ec.gateway.JWTUtils.JWTTokenUtils;
import com.ec.gateway.Service.JwtUserDetailsService;



@RestController
public class LoginController {

	public static String role = "";
	@Autowired
	private AuthenticationManager authenticationManager;


	@Autowired
	private JWTTokenUtils jwtTokenUtil;


	@Autowired
	private JwtUserDetailsService userDetailsService;

	@PostMapping(value="/ec/login",produces = { "application/json", "text/json" })
	public ResponseEntity<?> login(@RequestBody UserSignInData userData) throws Exception {
		LoginData loginData = new LoginData();

		authenticate(userData.getUserName(), userData.getPassword());
		final UserDetails userDetails = userDetailsService.loadUserByUsername(userData.getUserName());
		final String token = jwtTokenUtil.generateToken(userDetails);
		
		
		  Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities(); 
		  ArrayList<String> roles = new ArrayList<String>();
		  for(GrantedAuthority grantedAuthority : authorities) 
		  { 
			  roles.add(grantedAuthority.getAuthority());
		  }
		  String name = userDetails.getUsername();
		  return ResponseEntity.ok(new JwtResponse(name,token,roles));
	}

	private void authenticate(String username, String password) throws Exception {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (DisabledException e) {
			throw new Exception("USER_DISABLED", e);
		} catch (BadCredentialsException e) {
			throw new Exception("INVALID_CREDENTIALS", e);
		}
	}
}
