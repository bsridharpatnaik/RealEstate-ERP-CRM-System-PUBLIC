package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.Lead;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.StagnantStatsRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashboardService {
	
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	StagnantStatsRepo stagnantStatsRepo;
	
	public List returnLeadStatus() {
		// TODO Auto-generated method stub
		List data=new ArrayList<>();
		data=lRepo.getStatus();
		return data;
	}

	public  List<StagnantStats> returnStagnantStats() 
	{
		return stagnantStatsRepo.findAll();
	}

}
