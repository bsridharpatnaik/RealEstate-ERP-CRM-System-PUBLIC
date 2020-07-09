package com.ec.crm.Data;


import org.springframework.data.domain.Page;

import com.ec.crm.Model.Sentiment;


public class SentimentListWithTypeAheadData 
{
	Page<Sentiment> sentimentDetails;

	public Page<Sentiment> getSentimentDetails() {
		return sentimentDetails;
	}

	public void setSentimentDetails(Page<Sentiment> page) {
		this.sentimentDetails = page;
	}

	
	
	
	
}
