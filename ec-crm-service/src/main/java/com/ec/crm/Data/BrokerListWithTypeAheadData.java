package com.ec.crm.Data;


import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.crm.Model.Broker;

public class BrokerListWithTypeAheadData 
{
	Page<Broker> brokerDetails;
	List<String> typeAheadData;
	
	
	public List<String> getTypeAheadData() {
		return typeAheadData;
	}

	public void setTypeAheadData(List<String> typeAheadData) {
		this.typeAheadData = typeAheadData;
	}

	public Page<Broker> getBrokerDetails() {
		return brokerDetails;
	}

	public void setBrokerDetails(Page<Broker> brokerDetails) {
		this.brokerDetails = brokerDetails;
	}
	

	
	
	
}
