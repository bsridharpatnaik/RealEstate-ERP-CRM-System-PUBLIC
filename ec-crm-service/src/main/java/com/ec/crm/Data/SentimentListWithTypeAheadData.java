package com.ec.crm.Data;


import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.crm.Model.Sentiment;


public class SentimentListWithTypeAheadData 
{
	Page<Sentiment> sentimentDetails;
	List<String> sentimentTypeAhead;
	
	
	public List<String> getSentimentTypeAhead() {
		return sentimentTypeAhead;
	}

	public void setSentimentTypeAhead(List<String> sentimentTypeAhead) {
		this.sentimentTypeAhead = sentimentTypeAhead;
	}

	public Page<Sentiment> getSentimentDetails() {
		return sentimentDetails;
	}

	public void setSentimentDetails(Page<Sentiment> page) {
		this.sentimentDetails = page;
	}
}
