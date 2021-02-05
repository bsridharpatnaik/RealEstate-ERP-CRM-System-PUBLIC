
package com.ec.common.JWTUtils;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import com.ec.common.Service.JwtUserDetailsService;
import com.google.gson.Gson;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.Getter;
import lombok.Setter;

@Component
public class JwtRequestFilter extends OncePerRequestFilter
{

	@Autowired
	private JwtUserDetailsService jwtUserDetailsService;

	@Autowired
	private JWTTokenUtils jwtTokenUtil;

	private Gson gson = new Gson();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException
	{
		final String requestTokenHeader = request.getHeader("Authorization");
		String username = null;
		String jwtToken = null; // JWT Token is in the form "Bearer token". Remove Bearer word and get
		// only the Token
		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer "))
		{
			jwtToken = requestTokenHeader.substring(7);
			try
			{
				username = jwtTokenUtil.getUsernameFromToken(jwtToken);
			} catch (IllegalArgumentException e)
			{
				System.out.println("Unable to get JWT Token");
			} catch (ExpiredJwtException e)
			{
				System.out.println("JWT Token has expired");
			}
		} else
		{
			logger.warn("JWT Token does not begin with 	Bearer String");
		}
		// Once we get the token validate it.
		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null)
		{
			final String tenantId = request.getHeader("tenant-id");
			String path = new UrlPathHelper().getPathWithinApplication(request);
			if (StringUtils.isNotBlank(path) && !path.contains("/ism") && !path.contains("/user")
					&& !path.contains("tenant") && !path.contains("/notification") && !path.contains("/role")
					&& !path.contains("/token"))
			{
				username = username + "#" + tenantId;
			} else if (StringUtils.isBlank(path))
			{
				username = username + "#" + tenantId;
			}

			UserDetails userDetails = null;
			try
			{
				userDetails = this.jwtUserDetailsService.loadUserByUsername(username);
			} catch (JwtUserDetailsService.UserDataAccessException e)
			{
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write(gson.toJson(new Error("User not allowed to access data")));
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			// if token is valid configure Spring Security to manually set // authentication
			if (jwtTokenUtil.validateToken(jwtToken, userDetails))
			{
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				usernamePasswordAuthenticationToken
						.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				// After setting the Authentication in the context, we specify
				// that the current user is authenticated. So it passes the
				// Spring Security Configurations successfully.
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}
		}
		chain.doFilter(request, response);

	}

	@Setter
	@Getter
	public static class Error
	{
		private String message;

		public Error(String message)
		{
			super();
			this.message = message;
		}

	}
}
