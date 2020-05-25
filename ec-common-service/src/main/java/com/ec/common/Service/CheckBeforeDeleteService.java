package com.ec.common.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckBeforeDeleteService 
{
	@Autowired
	ContactService contactService;
	
	public Boolean checkIfContactIsUsed(Long id) throws Exception 
	{
		Boolean flag = contactService.checkIfContactUsed(id);
		System.out.println(flag.toString());
		return flag;
	}
}
