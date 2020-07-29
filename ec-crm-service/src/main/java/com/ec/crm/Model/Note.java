package com.ec.crm.Model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;

@Entity
@Table(name = "note")
public class Note extends ReusableFields{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "note_id", updatable = false, nullable = false)
	Long noteId;
	
	@Column(name="content")
	String content;
	
	@ManyToMany(fetch=FetchType.EAGER,cascade = CascadeType.ALL)
	@JoinTable(name = "note_fileinformation", joinColumns = {
			@JoinColumn(name = "note_id", referencedColumnName = "note_id") }, inverseJoinColumns = {
					@JoinColumn(name = "id", referencedColumnName = "id") })
	Set<FileInformation> fileInformations = new HashSet<>();
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="lead_id",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	@NotFound(action=NotFoundAction.IGNORE)
	Lead lead;
	
	@Column(name="pinned")
	Boolean pinned;
	
	@Column(name="creatorId")
	@JsonSerialize(using=ToUsernameSerializer.class)
	String creatorId;
	
	
	
	public Set<FileInformation> getFileInformations() {
		return fileInformations;
	}

	public void setFileInformations(Set<FileInformation> fileInformations) {
		this.fileInformations = fileInformations;
	}

	public Long getNoteId() {
		return noteId;
	}

	public void setNoteId(Long noteId) {
		this.noteId = noteId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Lead getLead() {
		return lead;
	}

	public void setLead(Lead lead) {
		this.lead = lead;
	}

	public Boolean getPinned() {
		return pinned;
	}

	public void setPinned(Boolean pinned) {
		this.pinned = pinned;
	}
	
	
	
}
