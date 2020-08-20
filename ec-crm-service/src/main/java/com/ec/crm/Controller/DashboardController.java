package com.ec.crm.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.PipelineAllReturnDAO;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Service.AllActivitiesService;
import com.ec.crm.Service.DashboardService;

@RestController
@RequestMapping(value="/dashboard",produces = { "application/json", "text/json" })
public class DashboardController {
	
	@Autowired
	DashboardService dashboardService;
	
	
	@GetMapping("/leadstatus")
	@ResponseStatus(HttpStatus.OK)
	public List returnLeadStatus()
	{
		return dashboardService.returnLeadStatus();
	}
	
	@GetMapping("/stagnant")
	@ResponseStatus(HttpStatus.OK)
	public List<StagnantStats> returnStagnantStats()
	{
		return dashboardService.returnStagnantStats();
	}
}
