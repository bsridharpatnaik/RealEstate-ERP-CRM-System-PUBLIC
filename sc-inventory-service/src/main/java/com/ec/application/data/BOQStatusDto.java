package com.ec.application.data;

public class BOQStatusDto {

	
	private String category;
	private String inventory;
	private double boqQuantity;
	private double outwardQuantity;
	private int status;
	
	
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getInventory() {
		return inventory;
	}
	public void setInventory(String inventory) {
		this.inventory = inventory;
	}
	public double getBoqQuantity() {
		return boqQuantity;
	}
	public void setBoqQuantity(double boqQuantity) {
		this.boqQuantity = boqQuantity;
	}
	public double getOutwardQuantity() {
		return outwardQuantity;
	}
	public void setOutwardQuantity(double outwardQuantity) {
		this.outwardQuantity = outwardQuantity;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	
	
	
}
