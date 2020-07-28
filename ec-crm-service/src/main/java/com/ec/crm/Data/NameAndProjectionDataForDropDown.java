package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.ReusableClasses.IdNameProjections;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class NameAndProjectionDataForDropDown 
{
	List<String> validPropertyType;
	List<String> validStatusType;
	List<IdNameProjections> brokerDetails;
	List<IdNameProjections> sourceDetails;
	List<IdNameProjections> sentimentDetails;
	public List<String> getValidPropertyType() {
		return validPropertyType;
	}
	public void setValidPropertyType(List<String> validPropertyType) {
		this.validPropertyType = validPropertyType;
	}
	public List<String> getValidStatusType() {
		return validStatusType;
	}
	public void setValidStatusType(List<String> validStatusType) {
		this.validStatusType = validStatusType;
	}
	public List<IdNameProjections> getBrokerDetails() {
		return brokerDetails;
	}
	public void setBrokerDetails(List<IdNameProjections> brokerDetails) {
		this.brokerDetails = brokerDetails;
	}
	public List<IdNameProjections> getSourceDetails() {
		return sourceDetails;
	}
	public void setSourceDetails(List<IdNameProjections> sourceDetails) {
		this.sourceDetails = sourceDetails;
	}
	public List<IdNameProjections> getSentimentDetails() {
		return sentimentDetails;
	}
	public void setSentimentDetails(List<IdNameProjections> sentimentDetails) {
		this.sentimentDetails = sentimentDetails;
	}
	
}
