package com.ec.common.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.ec.common.Data.CreateUserData;
import com.ec.common.Data.ResetPasswordData;
import com.ec.common.Data.UpdateRolesForUserData;
import com.ec.common.Data.UserListWithTypeAheadData;
import com.ec.common.Data.UserReturnData;
import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.UserSpecifications;
import com.ec.common.Model.Role;
import com.ec.common.Model.User;
import com.ec.common.Repository.RoleRepo;
import com.ec.common.Repository.UserRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {
	@Autowired
	UserRepo uRepo;

	@Autowired
	RoleRepo rRepo;

	//private static final Logger log = LoggerFactory.getLogger(UserService.class);
	public User createUser(CreateUserData userData) throws Exception {
		String username = userData.getUsername();
		String password = userData.getPassword();
		ArrayList<String> roles = userData.getRole();

		if (uRepo.findUserByUsername(username).size() == 0) {
			User user = new User();
			Set<Role> roleset = new HashSet<Role>();

			for (String role : roles) {
				Role roleEntity = rRepo.findByName(role);
				if(roleEntity!=null)
					roleset.add(roleEntity);
			}
			if(roleset.size()<1)
				throw new Exception("Role(s) not Found!");
			user.setUserName(username);
			user.setStatus(true);
			user.setRoles(roleset);
			user.setPassword(bCryptPassword(password));
			user.setPasswordExpired(false);
			uRepo.save(user);
			return user;

		} 
		else 
		{
			//log.info("User already exists");
			throw new Exception("User already exists");
		}
	}

	public String bCryptPassword(String password) {
		String bcyptedPassword;
		bcyptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		return bcyptedPassword;
	}

	public UserListWithTypeAheadData fetchAll(Pageable pageable) 
	{
		UserListWithTypeAheadData tpData  = new UserListWithTypeAheadData();
		tpData.setUserDetails(uRepo.findAll(pageable));
		tpData.setRoles(rRepo.findRoleNames());
		tpData.setUsernames(uRepo.findUserNames());
		return tpData;
	}

	public User resetPassword(ResetPasswordData rpData) throws Exception {
		String username = rpData.getUsername();
		String password = rpData.getNewPassword();
		ArrayList<User> users = uRepo.findUserByUsername(username);
		if (users.size() == 1) {
			User user = users.get(0);
			user.setPassword(bCryptPassword(password));
			uRepo.save(user);
			return user;
		} else {
			throw new Exception("No or Multiple users found by username!");
		}
	}

	public User updateRolesForUser(UpdateRolesForUserData upRoleData) throws Exception {
		String username = upRoleData.getUsername();
		ArrayList<String> roles = upRoleData.getRoles();
		ArrayList<User> users = uRepo.findUserByUsername(username);
		if (users.size() == 1) {
			User user = users.get(0);
			Set<Role> roleset = new HashSet<Role>();

			for (String role : roles) {
				Role roleEntity = rRepo.findByName(role);
				if (roleEntity != null)
					roleset.add(roleEntity);
			}
			user.setRoles(roleset);
			uRepo.save(user);
			return user;
		} else {
			throw new Exception("No or Multiple users found by username!");
		}
	}

	public UserReturnData fetchUserDetails() {
		UserReturnData userReturnData = new UserReturnData();
		List<String> roles = new ArrayList<String>();

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		userReturnData.setUsername(auth.getName());

		Collection<SimpleGrantedAuthority> authorities = (Collection<SimpleGrantedAuthority>) SecurityContextHolder
				.getContext().getAuthentication().getAuthorities();
		for (SimpleGrantedAuthority authority : authorities) {
			roles.add(authority.getAuthority());
		}
		userReturnData.setRoles(roles);
		return userReturnData;
	}

	public UserListWithTypeAheadData findFilteredUsers(FilterDataList contactFilterDataList, Pageable pageable) 
	{
		UserListWithTypeAheadData tpData  = new UserListWithTypeAheadData();
		
		tpData.setUserDetails(getFilteredData(contactFilterDataList,pageable));
		tpData.setRoles(rRepo.findRoleNames());
		tpData.setUsernames(uRepo.findUserNames());
		return tpData;
	}

	public Page<User> getFilteredData(FilterDataList contactFilterDataList, Pageable pageable)
	{
		Specification<User> spec = fetchSpecification(contactFilterDataList);
		if(spec!=null)
			return uRepo.findAll(spec, pageable);
		return uRepo.findAll(pageable);
	}
	
	private Specification<User> fetchSpecification(FilterDataList contactFilterDataList) 
	{
		Specification<User> specification = null;
		for(FilterAttributeData attrData:contactFilterDataList.getFilterData())
		{
			String attrName = attrData.getAttrName();
			List<String> attrValues = attrData.getAttrValue();
			
			Specification<User> internalSpecification = null;
			for(String attrValue : attrValues)
			{
				internalSpecification= internalSpecification==null?
						UserSpecifications.whereUsernameContains(attrValue)
						:internalSpecification.or(UserSpecifications.whereUsernameContains(attrValue));
			}
			specification= specification==null?internalSpecification:specification.and(internalSpecification);
		}
		return specification;
	}

	public User findSingleUserFromAll(long id) throws Exception 
	{
		Optional<User> user = uRepo.findById(id);
		if(user.isPresent())
			return user.get();
		else
			throw new Exception("User ID not found");
	}

	public User updateContact(Long id, CreateUserData payload) throws Exception 
	{
	    User user = findSingleUserFromAll(id);
	    String username = payload.getUsername();
	    if(!user.getUserName().equals(payload.getUsername()))
	    {
	    	if(uRepo.findUserNames().contains(username))
	    		throw new Exception("Username already exists");
	    }
	    
	    ArrayList < String > roles = payload.getRole();
	    Set < Role > roleset = new HashSet < Role > ();

	    for (String role: roles) {
	        Role roleEntity = rRepo.findByName(role);
	        if (roleEntity != null)
	            roleset.add(roleEntity);
	    }
	    if (roleset.size() < 1)
	        throw new Exception("Role(s) not Found!");
	    user.setUserName(username);
	    user.setStatus(true);
	    user.setRoles(roleset);
	    user.setPassword(bCryptPassword(payload.getPassword()));
	    user.setPasswordExpired(false);
	    uRepo.save(user);
	    return user;
	}
	
	
}
