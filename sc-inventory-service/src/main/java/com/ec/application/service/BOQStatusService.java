package com.ec.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.model.BOQStatus;
import com.ec.application.repository.BOQStatusRepo;

@Service
public class BOQStatusService
{

	@Autowired
	BOQStatusRepo boqStatusRepo;

	public List<BOQStatus> fetchAllBOQRecord()
	{
		return boqStatusRepo.findAll();
	}
}
