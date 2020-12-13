package com.ec.application.model;

import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.ec.application.data.CustomerTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@MappedSuperclass
@Audited
public class ContactMappedSuperClass
{
	public static final String SOFT_DELETED_CLAUSE = "is_deleted = 'false'";

	@Column(name = "is_deleted", columnDefinition = "BOOLEAN DEFAULT true")
	public boolean isDeleted;

	@NonNull
	@Column(nullable = false)
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String name;

	@NonNull
	@Column(nullable = false)
	String mobileNo;

	String emailId;

	@NonNull
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	CustomerTypeEnum contactType;

	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	@Column(name = "gst_number")
	String gstNumber;
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String contactPerson;
	String contactPersonMobileNo;
	String addr_line1;
	String addr_line2;
	String city;
	String state;
	String zip;

	@CreatedBy
	protected String createdBy;

	@CreatedDate
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Temporal(TIMESTAMP)
	protected Date creationDate;

	@LastModifiedBy
	protected String lastModifiedBy;

	@LastModifiedDate
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Temporal(TIMESTAMP)
	protected Date lastModifiedDate;

	public boolean isDeleted()
	{
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted)
	{
		this.isDeleted = isDeleted;
	}

	public static String getSoftDeletedClause()
	{
		return SOFT_DELETED_CLAUSE;
	}

	public String getName()
	{
		return name;
	}

	public String getCreatedBy()
	{
		return createdBy;
	}

	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}

	public Date getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(Date creationDate)
	{
		this.creationDate = creationDate;
	}

	public String getLastModifiedBy()
	{
		return lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy)
	{
		this.lastModifiedBy = lastModifiedBy;
	}

	public Date getLastModifiedDate()
	{
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate)
	{
		this.lastModifiedDate = lastModifiedDate;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getMobileNo()
	{
		return mobileNo;
	}

	public void setMobileNo(String mobileNo)
	{
		this.mobileNo = mobileNo;
	}

	public String getEmailId()
	{
		return emailId;
	}

	public void setEmailId(String emailId)
	{
		this.emailId = emailId;
	}

	public CustomerTypeEnum getContactType()
	{
		return contactType;
	}

	public void setContactType(CustomerTypeEnum contactType)
	{
		this.contactType = contactType;
	}

	public String getGstNumber()
	{
		return gstNumber;
	}

	public void setGstNumber(String gstNumber)
	{
		this.gstNumber = gstNumber;
	}

	public String getContactPerson()
	{
		return contactPerson;
	}

	public void setContactPerson(String contactPerson)
	{
		this.contactPerson = contactPerson;
	}

	public String getContactPersonMobileNo()
	{
		return contactPersonMobileNo;
	}

	public void setContactPersonMobileNo(String contactPersonMobileNo)
	{
		this.contactPersonMobileNo = contactPersonMobileNo;
	}

	public String getAddr_line1()
	{
		return addr_line1;
	}

	public void setAddr_line1(String addr_line1)
	{
		this.addr_line1 = addr_line1;
	}

	public String getAddr_line2()
	{
		return addr_line2;
	}

	public void setAddr_line2(String addr_line2)
	{
		this.addr_line2 = addr_line2;
	}

	public String getCity()
	{
		return city;
	}

	public void setCity(String city)
	{
		this.city = city;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public String getZip()
	{
		return zip;
	}

	public void setZip(String zip)
	{
		this.zip = zip;
	}

}
