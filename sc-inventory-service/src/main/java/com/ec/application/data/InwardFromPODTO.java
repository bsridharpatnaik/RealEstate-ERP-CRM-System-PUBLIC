package com.ec.application.data;

import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InwardFromPODTO {

    @NonNull
    private String poNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    @NonNull
    private Date inwardDate;

    private Boolean invoiceReceived;

    @JsonDeserialize(using = ToUpperCaseDeserializer.class)
    private String vehicleNo;
    private String supplierSlipNo;
    private String ourSlipNo;
    private String billNo;
    private String challanNo;
    private String noChallanBillReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    private Date challanDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    private Date billDate;

    private String additionalInfo;

    @NonNull
    List<FileInformationDAO> fileInformations;

    List<LineItemForInwardThroughPODTO> lineItems;
}
