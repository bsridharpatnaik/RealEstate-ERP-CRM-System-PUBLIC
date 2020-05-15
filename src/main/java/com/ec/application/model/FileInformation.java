package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.ReusableClasses.ReusableFields;

@Entity
@Table(name = "file_information")
@Audited
public class FileInformation 
{

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long id;
	@Column(name="fileuuid")
	String fileUUId;
	@Column(name="filename")
	String fileName;
	
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getFileUUId() {
		return fileUUId;
	}
	public void setFileUUId(String fileUUId) {
		this.fileUUId = fileUUId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
}
