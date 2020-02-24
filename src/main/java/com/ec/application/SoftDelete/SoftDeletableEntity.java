package com.ec.application.SoftDelete;

import javax.persistence.Column;

public class SoftDeletableEntity 
{

	public static final String SOFT_DELETED_CLAUSE = "is_deleted = 'false'";

    @Column(name="is_deleted", columnDefinition="BOOLEAN DEFAULT true")
    private boolean isDeleted;

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
