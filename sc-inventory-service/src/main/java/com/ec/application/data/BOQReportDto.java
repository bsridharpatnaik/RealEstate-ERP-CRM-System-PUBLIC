package com.ec.application.data;

public class BOQReportDto {
	
	private String buildingType;
	private String buildingUnit;
	private String inventory;
	private double boqQuantity;
	private double outwardQuantity;
	private String measurementUnit;
	private double excessQuantity;
	
	
	public double getExcessQuantity() {
		return excessQuantity;
	}
	public void setExcessQuantity(double excessQuantity) {
		this.excessQuantity = excessQuantity;
	}
	public String getMeasurementUnit() {
		return measurementUnit;
	}
	public void setMeasurementUnit(String measurementUnit) {
		this.measurementUnit = measurementUnit;
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
	public String getInventory() {
		return inventory;
	}
	public void setInventory(String inventory) {
		this.inventory = inventory;
	}
	public String getBuildingType() {
		return buildingType;
	}
	public void setBuildingType(String buildingType) {
		this.buildingType = buildingType;
	}
	public String getBuildingUnit() {
		return buildingUnit;
	}
	public void setBuildingUnit(String buildingUnit) {
		this.buildingUnit = buildingUnit;
	}
	
	
}
