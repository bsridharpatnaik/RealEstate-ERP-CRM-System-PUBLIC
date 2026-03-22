package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TenantStockDTO {
    private String tenantSchema;
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double quantity;
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double deadStock;
    /** Effective reorder level for this tenant (override if set, else global default). */
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double reorderLevel;
}
