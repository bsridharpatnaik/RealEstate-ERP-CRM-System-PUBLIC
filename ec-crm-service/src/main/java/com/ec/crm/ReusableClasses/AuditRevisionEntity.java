package com.ec.crm.ReusableClasses;



import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@RevisionEntity(AuditRevisionListener.class)
@Table(name = "REVINFO", schema = "audit")
@AttributeOverrides({
        @AttributeOverride(name = "timestamp", column = @Column(name = "REVTSTMP")),
        @AttributeOverride(name = "id", column = @Column(name = "REV"))})
@Getter
@Setter
public class AuditRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "USERID", nullable = false)
    private Long userId;

    @Column(name = "USERNAME", nullable = false)
    private String userName;
    
    
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
}
