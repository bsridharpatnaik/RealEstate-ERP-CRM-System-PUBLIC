package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.springframework.lang.NonNull;

import com.ec.crm.ReusableClasses.ReusableFields;

import lombok.Data;

@Entity
@Table(name = "NotificationSent")
@Data
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class NotificationSent extends ReusableFields implements Serializable
{

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "notification_id", updatable = false, nullable = false)
	Long notificationId;


	@Column(name = "status")
	@NonNull
	String status;
	
	
	@Column(name = "leadactivity_id")
	@NonNull
	Long leadActivityId;


		public NotificationSent()
	{
		super();
		// TODO Auto-generated constructor stub
	}
}
