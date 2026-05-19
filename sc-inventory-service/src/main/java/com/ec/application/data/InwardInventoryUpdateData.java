package com.ec.application.data;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class InwardInventoryUpdateData {

    @NonNull
    Long supplierId;

    @NonNull
    List<ProductAndQuantity> productWithQuantities;

    @JsonDeserialize(using = ToUpperCaseDeserializer.class)
    String vehicleNo;

    String supplierSlipNo;

    String ourSlipNo;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    String additionalInfo;

    Boolean invoiceReceived;

    String challanNo;

    String billNo;

    String noChallanBillReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    Date inwardDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    Date challanDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    Date billDate;

    List<FileInformationDAO> fileInformations;
}

