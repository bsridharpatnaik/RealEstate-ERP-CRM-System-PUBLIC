package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateData {
    Long categoryId;
    @NonNull
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String productName;
    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    String productDescription;
    @NonNull
    Double reorderQuantity;
    String measurementUnit;
    Boolean showOnDashboard;
    Boolean isManagedInventory;
    Boolean isExpirable;
}
