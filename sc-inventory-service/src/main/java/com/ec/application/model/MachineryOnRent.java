package com.ec.application.model;

import java.util.Date;
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

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "machineryonrent")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Data
@NoArgsConstructor
public class MachineryOnRent extends ReusableFields
{

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long morid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(nullable = false)
	@NonNull
	Date date;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "machineryId", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Machinery machinery;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "supplierId", referencedColumnName = "contactId", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Supplier supplier;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "contractorId", referencedColumnName = "contactId", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Contractor contractor;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "locationId", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	UsageLocation usageLocation;

	String vehicleNo;

	String additionalNotes;

	MORRentModeEnum mode;

	@JsonProperty("startDateTime")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
	Date startDateTime;

	@JsonProperty("endDateTime")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
	Date endDateTime;

	@JsonProperty("startDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date startDate;

	@JsonProperty("endDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date endDate;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "mor_fileinformation", joinColumns =
	{ @JoinColumn(name = "morid", referencedColumnName = "morid") }, inverseJoinColumns =
	{ @JoinColumn(name = "id", referencedColumnName = "id") })
	Set<FileInformation> fileInformations = new HashSet<>();

	Double initialMeterReading;
	Double endMeterReading;
	Double noOfTrips;
	Double rate;
	Double amountCharged;
}
