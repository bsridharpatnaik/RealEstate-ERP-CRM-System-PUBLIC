package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.SoftDelete.SoftDeletableEntity;

@Entity
@Table(name = "Vendor")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class Vendor extends SoftDeletableEntity
{
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long vendorid;
	
	@NonNull
	@Column(unique=true)
	String VendorName;
	
	String VendorAddress;
	String VendorPhone;
	String VendorEmail;
}
