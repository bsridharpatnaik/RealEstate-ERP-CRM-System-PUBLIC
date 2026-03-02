package com.ec.application.model;

import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.Date;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@MappedSuperclass
@Access(AccessType.FIELD)
@Audited
@Getter
@Setter
@NoArgsConstructor
public class ContactMappedSuperClass {

	public static final String SOFT_DELETED_CLAUSE = "is_deleted = 'false'";

	@Column(
			name = "is_deleted",
			nullable = false,
			columnDefinition = "BOOLEAN DEFAULT true"
	)
	private boolean isDeleted;

	@NonNull
	@Column(name = "name", nullable = false, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String name;

	@Column(name = "mobileNo", nullable = true, length = 255)
	private String mobileNo;

	@Column(name = "emailId", nullable = true, length = 255)
	private String emailId;

	@Column(name = "contactType", nullable = true, length = 255)
	private String contactType;

	@Column(name = "gst_number", nullable = true, length = 255)
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	private String gstNumber;

	@Column(name = "contactPerson", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String contactPerson;

	@Column(name = "contactPersonMobileNo", nullable = true, length = 255)
	private String contactPersonMobileNo;

	@Column(name = "addr_line1", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String addr_line1;

	@Column(name = "addr_line2", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String addr_line2;

	@Column(name = "city", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String city;

	@Column(name = "state", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String state;

	@Column(name = "zip", nullable = true, length = 255)
	private String zip;

	@Column(name = "account_name", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String accountName;

	@Column(name = "account_number", nullable = true, length = 50)
	private String accountNumber;

	@Column(name = "bank_name", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String bankName;

	@Column(name = "branch_name", nullable = true, length = 255)
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	private String branchName;

	@Column(name = "ifsc_code", nullable = true, length = 20)
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	private String ifscCode;

	@CreatedBy
	@Column(name = "createdBy", nullable = true, length = 255, updatable = false)
	protected String createdBy;

	@CreatedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creationDate", nullable = true, updatable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	protected Date creationDate;

	@LastModifiedBy
	@Column(name = "lastModifiedBy", nullable = true, length = 255)
	protected String lastModifiedBy;

	@LastModifiedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "lastModifiedDate", nullable = true)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	protected Date lastModifiedDate;

	@Column(name = "is_system_contact", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
	private boolean isSystemContact = false;
}