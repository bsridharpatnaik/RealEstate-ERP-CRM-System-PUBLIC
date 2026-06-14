package com.ec.application.data;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

@Data
public class UpdatePoRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date poDate;
    private Long supplierId;
    private Long firmId;
    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    private String subject;
    private String notes;
    private Double grandTotal;
    private Double freightCharges;
    private Double freightGstPercent;
    private Double totalFreightCharges;
    @Column(length = 35)
    private String overridePhoneNumber;
    private String overrideEmail;
    private boolean specialPo = false;
    private String projectName;
    private List<FileInformationDAO> fileInformations;
    private List<CustomChargeRequest> customCharges;
    /** Rate/discount/gst updates per existing line — quantity and indent refs are NOT changed */
    private List<UpdatePoLineRequest> lineUpdates;
}
