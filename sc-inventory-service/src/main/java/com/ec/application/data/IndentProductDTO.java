package com.ec.application.data;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
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
}
