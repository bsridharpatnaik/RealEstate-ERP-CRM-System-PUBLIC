package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceOrderRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    @NonNull
    private Date serviceDate;
    private Long vendorId;
    private Long firmId;
    private String subject;
    private String notes;
    private Double grandTotal;
    @Column(length = 35)
    private String overridePhoneNumber;
    private String overrideEmail;
    private String projectName;
    private Double specialDiscount;
    /** One entry = one service line */
    private List<CreateServiceOrderLineRequest> lineItems;
    private List<FileInformationDAO> fileInformations;
}
