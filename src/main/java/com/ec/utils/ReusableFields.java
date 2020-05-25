package com.ec.utils;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@MappedSuperclass
@Audited
public class ReusableFields implements Serializable
{

	public static final String SOFT_DELETED_CLAUSE = "is_deleted = 'false'";

	
    @Column(name="is_deleted", columnDefinition="BOOLEAN DEFAULT true")
    public boolean isDeleted;

    @CreationTimestamp
	@Column(name = "created_at")
	@JsonProperty("created")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss ")
	private Date created;
	
	
	@Column(name = "updated_at")
	@JsonProperty("updated")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
	@UpdateTimestamp
	private Date modified;
	
	
	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

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
