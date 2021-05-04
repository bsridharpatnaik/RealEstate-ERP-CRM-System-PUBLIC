package com.ec.crm.SubClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.ec.crm.Enums.InstanceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.crm.Data.PipelineAllReturnDAO;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Service.AllActivitiesService;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SetDataForPipeline implements Runnable
{
	private CyclicBarrier barrier;
	private PipelineAllReturnDAO pipelineAllReturnDAO = new PipelineAllReturnDAO();
	private List<Lead> leads = new ArrayList<Lead>();
	Logger log = LoggerFactory.getLogger(SetLeadGenerated.class);
	private HashMap<Long, LeadActivity> leadRecentActivityMapping;
	LeadStatusEnum status;
	InstanceEnum instance;
	public SetDataForPipeline(CyclicBarrier barrier, PipelineAllReturnDAO pipelineAllReturnDAO, List<Lead> leads,
			HashMap<Long, LeadActivity> leadRecentActivityMapping, LeadStatusEnum status,InstanceEnum instance1)
	{
		this.status = status;
		this.pipelineAllReturnDAO = pipelineAllReturnDAO;
		this.barrier = barrier;
		this.leads = leads;
		this.leadRecentActivityMapping = leadRecentActivityMapping;
		this.instance=instance1;
	}

	@Override
	public void run()
	{

		AllActivitiesService allActivitiesService = new AllActivitiesService();

		log.info("Fetching stats for Lead Generated for pipeline");
		try
		{
			if (status.equals(LeadStatusEnum.New_Lead))
				pipelineAllReturnDAO.setLeadGeneration(allActivitiesService.fetchPipelineDataFromActivityList(leads,
						LeadStatusEnum.New_Lead, leadRecentActivityMapping));
			else if (status.equals(LeadStatusEnum.Negotiation))
				pipelineAllReturnDAO.setNegotiation(allActivitiesService.fetchPipelineDataFromActivityList(leads,
						LeadStatusEnum.Negotiation, leadRecentActivityMapping));
			else if (status.equals(LeadStatusEnum.Property_Visit))
				pipelineAllReturnDAO.setPropertyVisit(allActivitiesService.fetchPipelineDataFromActivityList(leads,
						LeadStatusEnum.Property_Visit, leadRecentActivityMapping));
			else if (status.equals(LeadStatusEnum.Deal_Closed))
				pipelineAllReturnDAO.setDeal_close(allActivitiesService.fetchPipelineDataFromActivityList(leads,
						LeadStatusEnum.Deal_Closed, leadRecentActivityMapping));
		} catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		log.info("Completed stats for Lead Generated");
		try
		{
			barrier.await();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
