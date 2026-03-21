package com.ec.application.data;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import lombok.Data;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

@Data
public class IndentProductDTO {

    @NonNull
    Long productId;

    @NonNull
    Double quantity;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    String specification;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    String remarks;

    @JsonDeserialize(using = ToUpperCaseDeserializer.class)
    String measurementUnit;

    private String lineItemCode;  // Will be null for new items during creation
    private String parentLineItemCode;

    /** Optional per-line-item expected delivery date (overrides header-level needByDate). */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    private Date needByDate;
}
