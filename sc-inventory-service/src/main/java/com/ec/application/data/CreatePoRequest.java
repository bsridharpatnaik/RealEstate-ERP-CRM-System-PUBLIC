package com.ec.application.data;

import java.util.*;
import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import javax.persistence.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePoRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    @NonNull
    private Date poDate;
    private Long supplierId;
    private Long firmId;
    @JsonDeserialize(using= ToSentenceCaseDeserializer.class)
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
    /** One entry = one PO line */
    private List<CreatePoLineRequest> lineItems;
    @NonNull
    List<FileInformationDAO> fileInformations;
}

