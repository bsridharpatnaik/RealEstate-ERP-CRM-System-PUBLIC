package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ec.ReusableClasses.ReusableFields;

@Entity
@Table(name = "sentiment")
public class Sentiment extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "sentiment_id", updatable = false, nullable = false)
	Long sentimentId;
	String name;
	String description;
	public Long getSentimentId() {
		return sentimentId;
	}
	public void setSentimentId(Long sentimentId) {
		this.sentimentId = sentimentId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}
