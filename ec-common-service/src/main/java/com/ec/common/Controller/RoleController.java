package com.ec.common.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Model.Role;
import com.ec.common.Service.RoleService;


@RestController
@RequestMapping(value="/role",produces = { "application/json", "text/json" })
public class RoleController 
{
	@Autowired
	RoleService roleService;
	
	@GetMapping
	public Page<Role> returnAllRoles(Pageable pageable) 
	{
		
		return roleService.findAll(pageable);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Role createRole(@RequestBody Role payload) throws Exception{
		
		return roleService.createRole(payload);
	}
}
