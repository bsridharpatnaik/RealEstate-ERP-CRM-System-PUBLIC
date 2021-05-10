package com.ec.crm.Data;


import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.crm.Model.Broker;

import lombok.Data;

@Data
public class BrokerListWithTypeAheadData 
{
	Page<Broker> brokerDetails;
	List<String> typeAheadData;
}
