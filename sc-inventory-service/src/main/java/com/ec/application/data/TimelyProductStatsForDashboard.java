package com.ec.application.data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

@Data
public class TimelyProductStatsForDashboard {
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double daily;
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double weekly;
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double monthly;
}
