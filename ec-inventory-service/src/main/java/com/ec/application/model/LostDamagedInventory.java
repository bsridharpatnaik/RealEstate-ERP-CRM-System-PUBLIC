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

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table(name = "lost_damaged_inventory")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class LostDamagedInventory extends ReusableFields
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long lostdamagedid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date date;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "productId", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Product product;

	@NonNull
	Double quantity;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double closingStock;

	@NonNull
	String locationOfTheft;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "warehouseName", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	@NonNull
	Warehouse warehouse;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "lostdamaged_fileinformation", joinColumns =
	{ @JoinColumn(name = "lostdamagedid", referencedColumnName = "lostdamagedid") }, inverseJoinColumns =
	{ @JoinColumn(name = "id", referencedColumnName = "id") })
	Set<FileInformation> fileInformations = new HashSet<>();

	String additionalComment;

	public Long getLostdamagedid()
	{
		return lostdamagedid;
	}

	public void setLostdamagedid(Long lostdamagedid)
	{
		this.lostdamagedid = lostdamagedid;
	}

	public String getAdditionalComment()
	{
		return additionalComment;
	}

	public void setAdditionalComment(String additionalComment)
	{
		this.additionalComment = additionalComment;
	}

	public Set<FileInformation> getFileInformations()
	{
		return fileInformations;
	}

	public void setFileInformations(Set<FileInformation> fileInformations)
	{
		this.fileInformations = fileInformations;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public Warehouse getWarehouse()
	{
		return warehouse;
	}

	public void setWarehouse(Warehouse warehouse)
	{
		this.warehouse = warehouse;
	}

	public Long getId()
	{
		return lostdamagedid;
	}

	public void setId(Long id)
	{
		id = id;
	}

	public Product getProduct()
	{
		return product;
	}

	public void setProduct(Product product)
	{
		this.product = product;
	}

	public Double getQuantity()
	{
		return quantity;
	}

	public void setQuantity(Double quantity)
	{
		this.quantity = quantity;
	}

	public Double getClosingStock()
	{
		return closingStock;
	}

	public void setClosingStock(Double closingStock)
	{
		this.closingStock = closingStock;
	}

	public String getLocationOfTheft()
	{
		return locationOfTheft;
	}

	public void setLocationOfTheft(String locationOfTheft)
	{
		this.locationOfTheft = locationOfTheft;
	}
}
