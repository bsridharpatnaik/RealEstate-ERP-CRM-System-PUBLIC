package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.crm.Data.AssigneeDAO;
import com.ec.crm.ReusableClasses.ReusableFields;

import lombok.Data;

@Entity
@Table(name = "file_information")
@Audited
@Data
public class FileInformation 
{

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long id;
	@Column(name="fileuuid")
	String fileUUId;
	@Column(name="filename")
	String fileName;
}
