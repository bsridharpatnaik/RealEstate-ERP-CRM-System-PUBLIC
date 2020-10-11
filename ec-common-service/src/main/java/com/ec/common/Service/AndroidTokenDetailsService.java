package com.ec.common.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ec.common.Data.AndroidTokenDetailsDTO;
import com.ec.common.Model.AndroidTokenDetails;
import com.ec.common.Repository.AndroidTokenDetailsRepo;
import com.ec.common.Repository.UserRepo;

@Service
public class AndroidTokenDetailsService
{

	@Autowired
	UserRepo userRepo;

	@Autowired
	AndroidTokenDetailsRepo tokenRepo;

	public void updateTokenForUser(String token) throws Exception
	{
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long userId = userRepo.findId(auth.getName());
		int userCount = tokenRepo.findCountByUserId(userId);
		if (userCount > 1)
		{
			throw new Exception("More than one token records found for same user.");
		} else if (userCount == 1)
		{
			List<AndroidTokenDetails> atdList = tokenRepo.findByUserId(userId);
			AndroidTokenDetails atd = atdList.get(0);
			atd.setToken(token);
			tokenRepo.save(atd);
		} else if (userCount == 0)
		{
			AndroidTokenDetails atd = new AndroidTokenDetails();
			atd.setToken(token);
			atd.setUser(userRepo.findById(userId).get());
			tokenRepo.save(atd);
		}
	}

	public List<AndroidTokenDetailsDTO> fetchAllTokens()
	{
		List<AndroidTokenDetails> atdList = new ArrayList<AndroidTokenDetails>();
		List<AndroidTokenDetailsDTO> atdDTOList = new ArrayList<AndroidTokenDetailsDTO>();
		atdList = tokenRepo.findAll();
		for (AndroidTokenDetails atd : atdList)
		{
			AndroidTokenDetailsDTO atdDto = new AndroidTokenDetailsDTO();
			atdDto.setUserName(atd.getUser().getUserName());
			atdDto.setToken(atd.getToken());
			atdDTOList.add(atdDto);
		}
		return atdDTOList;
	}
}
