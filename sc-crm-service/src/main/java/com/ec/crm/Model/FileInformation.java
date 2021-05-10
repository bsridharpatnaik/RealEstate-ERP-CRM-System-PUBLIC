package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_information")
@Audited
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInformation
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;
	@Column(name = "fileuuid")
	String fileUUId;
	@Column(name = "filename")
	String fileName;

	public FileInformation(String fileUUId, String fileName)
	{
		super();
		this.fileUUId = fileUUId;
		this.fileName = fileName;
	}
}
