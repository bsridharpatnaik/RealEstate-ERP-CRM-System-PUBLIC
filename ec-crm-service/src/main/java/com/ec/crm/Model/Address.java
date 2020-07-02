package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ec.ReusableClasses.ReusableFields;


@Entity
@Table(name = "address")
public class Address extends ReusableFields implements Serializable 
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "addr_id", updatable = false, nullable = false)
	Long addrId;
	String addr_line1;
	String addr_line2;
	String city;
	String district;
	String pincode;
	public Long getAddrId() {
		return addrId;
	}
	public void setAddrId(Long addrId) {
		this.addrId = addrId;
	}
	public String getAddr_line1() {
		return addr_line1;
	}
	public void setAddr_line1(String addr_line1) {
		this.addr_line1 = addr_line1;
	}
	public String getAddr_line2() {
		return addr_line2;
	}
	public void setAddr_line2(String addr_line2) {
		this.addr_line2 = addr_line2;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getDistrict() {
		return district;
	}
	public void setDistrict(String district) {
		this.district = district;
	}
	public String getPincode() {
		return pincode;
	}
	public void setPincode(String pincode) {
		this.pincode = pincode;
	}
	
	
}