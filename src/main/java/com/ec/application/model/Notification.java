package com.ec.application.model;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.ReusableClasses.ReusableFields;

@Entity
@Table(name = "notofictaion")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class Notification 
{
	
}
