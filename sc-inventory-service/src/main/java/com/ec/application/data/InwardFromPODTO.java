package com.ec.application.data;

import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

@Data
public class InwardFromPODTO {

    @NonNull
    private String PONumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @NonNull
    private Date inwardDate;

    private Boolean invoiceReceived;

    @JsonDeserialize(using = ToUpperCaseDeserializer.class)
    private String vehicleNo;
    private String supplierSlipNo;
    private String ourSlipNo;
    private String billNo;
    private String challanNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date challanDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date billDate;

    private String additionalInfo;

    @NonNull
    List<FileInformationDAO> fileInformations;

    List<LineItemForInwardThroughPODTO> lineItems;
}
