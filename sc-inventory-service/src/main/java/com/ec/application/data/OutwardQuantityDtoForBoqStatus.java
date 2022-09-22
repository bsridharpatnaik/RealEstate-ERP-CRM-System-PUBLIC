package com.ec.application.data;

public class OutwardQuantityDtoForBoqStatus{

    Integer id;
    Double outwardQuantity;
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Double getOutwardQuantity() {
		return outwardQuantity;
	}
	public void setOutwardQuantity(Double outwardQuantity) {
		this.outwardQuantity = outwardQuantity;
	}
	public OutwardQuantityDtoForBoqStatus(Integer id, Double outwardQuantity) {
		super();
		this.id = id;
		this.outwardQuantity = outwardQuantity;
	}
	public OutwardQuantityDtoForBoqStatus() {
		super();
	}
	
	
	
}
