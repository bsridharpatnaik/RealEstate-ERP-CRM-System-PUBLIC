package com.ec.common.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Data.CreateUserData;
import com.ec.common.Data.ResetPasswordData;
import com.ec.common.Data.UpdateRolesForUserData;
import com.ec.common.Data.UserListWithTypeAheadData;
import com.ec.common.Data.UserReturnData;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Model.User;
import com.ec.common.Service.UserService;

@RestController
@RequestMapping(value = "/user", produces =
{ "application/json", "text/json" })
public class UserController
{

	@Autowired
	UserService userService;

	@GetMapping
	@PreAuthorize("hasAuthority('admin')")
	public UserListWithTypeAheadData returnAllUsers(Pageable pageable)
	{

		return userService.fetchAll(pageable);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public UserListWithTypeAheadData returnFilteredContacts(@RequestBody FilterDataList contactFilterDataList,
			@PageableDefault(page = 0, size = 10, sort = "userId", direction = Direction.DESC) Pageable pageable)
	{
		return userService.findFilteredUsers(contactFilterDataList, pageable);
	}

	@PostMapping("/create")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('admin')")
	public User createUser(@RequestBody CreateUserData payload) throws Exception
	{

		return userService.createUser(payload);
	}

	@PostMapping("/updatepassword")
	@PreAuthorize("hasAuthority('admin')")
	@ResponseStatus(HttpStatus.OK)
	public User updateUser(@RequestBody ResetPasswordData payload) throws Exception
	{

		return userService.resetPassword(payload);
	}

	@GetMapping("/{id}")
	public User findUserByID(@PathVariable long id) throws Exception
	{
		return userService.findSingleUserFromAll(id);
	}

	@GetMapping("/allowedtenants")
	public List<String> findTenantsForUser() throws Exception
	{
		return userService.findTenantsForUser();
	}

	@GetMapping("/byid/{id}")
	public UserReturnData findLimitedDetailsForUserByID(@PathVariable long id) throws Exception
	{
		return userService.fetchUserDetailsById(id);
	}

	@GetMapping("/byname/{name}")
	public UserReturnData findLimitedDetailsForUserByID(@PathVariable String name) throws Exception
	{
		return userService.fetchUserDetailsByName(name);
	}

	@GetMapping("/list")
	public List<UserReturnData> findLimitedDetailsForUserByID() throws Exception
	{
		return userService.fetchUserList();
	}

	@PostMapping("/updateroles")
	@PreAuthorize("hasAuthority('admin')")
	@ResponseStatus(HttpStatus.OK)
	public User updateRolesForUser(@RequestBody UpdateRolesForUserData payload) throws Exception
	{

		return userService.updateRolesForUser(payload);
	}

	@GetMapping(value = "/me", produces = "application/json")
	public UserReturnData returnUserName()
	{
		return userService.fetchUserDetails();
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('admin')")
	public User updateUser(@PathVariable Long id, @RequestBody CreateUserData payload) throws Exception
	{
		return userService.updateUser(id, payload);
	}
}
