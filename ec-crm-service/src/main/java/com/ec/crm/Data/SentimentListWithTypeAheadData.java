package com.ec.crm.Data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.crm.Model.Sentiment;
import com.ec.crm.Model.Source;

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
