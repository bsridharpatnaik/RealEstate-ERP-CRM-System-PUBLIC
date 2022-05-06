package com.ec.crm.Data;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChartDataDTO {

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double total;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double remaining;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double received;

    public ChartDataDTO(double total, double received) {
        this.total = total;
        this.received = received;
        this.remaining = total - received;
    }
}
