package com.ec.application.ReusableClasses;

import javax.persistence.*;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@RevisionEntity(AuditRevisionListener.class)
@Table(name = "REVINFO")
@Getter
@Setter
public class AuditRevisionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@RevisionNumber
	@Column(name = "REV")
	private int id;

	@RevisionTimestamp
	@Column(name = "REVTSTMP")
	private long timestamp;

	@Column(name = "USERID", nullable = false)
	private Long userId;

	@Column(name = "USERNAME", nullable = false)
	private String userName;

	public void setUserId(Long userId) {
		this.userId = userId == null ? 0 : userId;
	}
}
