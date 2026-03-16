package com.ec.application.data;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InwardInventoryData {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    @NonNull
    Date inwardDate;

    @NonNull
    Long supplierId;

    @NonNull
    List<ProductWithQuantity> productWithQuantities;

    @JsonDeserialize(using = ToUpperCaseDeserializer.class)
    String vehicleNo;

    String supplierSlipNo;

    String ourSlipNo;

    private Boolean isSampleInward = false;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    String additionalInfo;

    Boolean invoiceReceived;

    String challanNo;

    String billNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    Date challanDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    Date billDate;

    List<FileInformationDAO> fileInformations;
}
