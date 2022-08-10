package com.ec.application.model;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import com.ec.application.ReusableClasses.*;

@Entity
@Table(name = "indent")
@Audited
//@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class Indent {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long indentNumber;
	
	private int noOfInventory;
	
	private long poNumber;
	
	private String indentStatus;
	
	private Timestamp creationDate;
	
	public long getIndentNumber() {
		return indentNumber;
	}
	public void setIndentNumber(long indentNumber) {
		this.indentNumber = indentNumber;
	}
	public Timestamp getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Timestamp creationDate) {
		this.creationDate = creationDate;
	}
	public int getNoOfInventory() {
		return noOfInventory;
	}
	public void setNoOfInventory(int noOfInventory) {
		this.noOfInventory = noOfInventory;
	}
	public long getPoNumber() {
		return poNumber;
	}
	public void setPoNumber(long poNumber) {
		this.poNumber = poNumber;
	}
	public String getIndentStatus() {
		return indentStatus;
	}
	public void setIndentStatus(String indentStatus) {
		this.indentStatus = indentStatus;
	}
	
	

}
