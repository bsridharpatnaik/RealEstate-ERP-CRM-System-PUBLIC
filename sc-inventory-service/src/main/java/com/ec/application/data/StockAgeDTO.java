package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import java.util.Date;
@Data
public class StockAgeDTO {
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    double quantity;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date date;

    String age;

    public StockAgeDTO(double quantity, Date entryDate, String age) {
        this.quantity = quantity;
        this.date = entryDate;
        this.age = age;
    }
}
