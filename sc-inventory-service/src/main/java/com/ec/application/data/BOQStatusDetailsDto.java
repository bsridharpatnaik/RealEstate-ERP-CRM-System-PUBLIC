package com.ec.application.data;

public class BOQStatusDetailsDto {

    private BOQStatusDetailsMapKey BOQStatusDetailsMapKey;
    private Long boqUploadId;
    private String finalLocation;
    private Double boqQuantity;
    private Double outwardQuantity;

    public Long getBoqUploadId() { return boqUploadId; }
    public void setBoqUploadId(Long boqUploadId) { this.boqUploadId = boqUploadId; }

    public String getFinalLocation() {
        return finalLocation;
    }

    public void setFinalLocation(String finalLocation) {
        this.finalLocation = finalLocation;
    }

    public Double getBoqQuantity() {
        return boqQuantity;
    }

    public void setBoqQuantity(Double boqQuantity) {
        this.boqQuantity = boqQuantity;
    }

    public Double getOutwardQuantity() {
        return outwardQuantity;
    }

    public void setOutwardQuantity(Double outwardQuantity) {
        this.outwardQuantity = outwardQuantity;
    }


    public BOQStatusDetailsMapKey getBOQStatusDetailsMapKey() {
        return BOQStatusDetailsMapKey;
    }

    public void setBOQStatusDetailsMapKey(BOQStatusDetailsMapKey bOQStatusDetailsMapKey) {
        BOQStatusDetailsMapKey = bOQStatusDetailsMapKey;
    }

    public BOQStatusDetailsDto() {
        super();
    }

    @Override
    public String toString() {
        return "BOQStatusDetailsDto [BOQStatusDetailsMapKey=" + BOQStatusDetailsMapKey + ", finalLocation="
                + finalLocation + ", boqQuatity=" + boqQuantity + ", outwardQuantity=" + outwardQuantity + "]";
    }


}
