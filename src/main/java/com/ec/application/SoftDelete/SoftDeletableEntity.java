package com.ec.application.SoftDelete;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;

@MappedSuperclass
@Audited
public class SoftDeletableEntity implements Serializable
{

	public static final String SOFT_DELETED_CLAUSE = "is_deleted = 'false'";

	
    @Column(name="is_deleted", columnDefinition="BOOLEAN DEFAULT true")
    public boolean isDeleted;

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public static String getSoftDeletedClause() {
		return SOFT_DELETED_CLAUSE;
	}
    
}
