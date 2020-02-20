package com.ec.application.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ec.application.data.LoginData;
import com.ec.application.exception.ValidationError;
import com.ec.application.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	
	ObjectMapper mapper = new ObjectMapper();
	private AuthenticationManager authenticationManager;
	private boolean postOnly = true;

	private TokenProvider tokenProvider;

	public JWTAuthenticationFilter(AuthenticationManager authenticationManager, TokenProvider tokenProvider) {
		this.authenticationManager=authenticationManager;
		this.tokenProvider=tokenProvider;
	}

	public JWTAuthenticationFilter(AuthenticationManager authenticationManager2,
			Class<UsernamePasswordAuthenticationFilter> class1) {

	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		
		if (postOnly && !request.getMethod().equals("POST")) {
			throw new AuthenticationServiceException(
					"Authentication method not supported: " + request.getMethod());
		}

		String username = request.getParameter("userName");
		String password = request.getParameter("password");

		if (username == null) {
			username = "";
		}

		if (password == null) {
			password = "";
		}

		username = username.trim();

		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);

		// Allow subclasses to set the "details" property
		setDetails(request, authRequest);

		return this.getAuthenticationManager().authenticate(authRequest);
	}
	
	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {

		// Create standard Response Object
		LoginData status = new LoginData();
		status.setStatus(false);
		status.addError("Auth Error" ,new ValidationError("JWT Token", failed.getMessage(), "401"));

		// Set response Object to response
		String exceptionJson = mapper.writeValueAsString(status);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.getWriter().write(exceptionJson);
		response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
		response.setContentType(MediaType.APPLICATION_JSON.toString());
	}

}
