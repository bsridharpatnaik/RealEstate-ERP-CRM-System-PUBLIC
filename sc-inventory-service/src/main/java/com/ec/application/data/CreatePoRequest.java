package com.ec.application.data;

import java.util.*;
import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
public class CreatePoRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @NonNull
    private Date poDate;
    private Long supplierId;
    private Long firmId;
    @JsonDeserialize(using= ToSentenceCaseDeserializer.class)
    private String subject;
    @JsonDeserialize(using= ToSentenceCaseDeserializer.class)
    private String notes;
    private Double grandTotal;

    /** One entry = one PO line */
    private List<CreatePoLineRequest> lineItems;
}

