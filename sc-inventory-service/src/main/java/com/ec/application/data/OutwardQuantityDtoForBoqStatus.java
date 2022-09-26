package com.ec.application.data;

public class OutwardQuantityDtoForBoqStatus{

	BOQStatusDetailsMapKey bOQStatusDetailsMapKey;
    Double outwardQuantity;

	public Double getOutwardQuantity() {
		return outwardQuantity;
	}
	public void setOutwardQuantity(Double outwardQuantity) {
		this.outwardQuantity = outwardQuantity;
	}

	public OutwardQuantityDtoForBoqStatus() {
		super();
	}
	public BOQStatusDetailsMapKey getbOQStatusDetailsMapKey() {
		return bOQStatusDetailsMapKey;
	}
	public void setbOQStatusDetailsMapKey(BOQStatusDetailsMapKey bOQStatusDetailsMapKey) {
		this.bOQStatusDetailsMapKey = bOQStatusDetailsMapKey;
	}
	
	
	
}
