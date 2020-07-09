package com.ec.crm.Data;


import org.springframework.data.domain.Page;

import com.ec.crm.Model.Broker;

public class BrokerListWithTypeAheadData 
{
	Page<Broker> brokerDetails;

	public Page<Broker> getBrokerDetails() {
		return brokerDetails;
	}

	public void setBrokerDetails(Page<Broker> brokerDetails) {
		this.brokerDetails = brokerDetails;
	}
	

	
	
	
}
