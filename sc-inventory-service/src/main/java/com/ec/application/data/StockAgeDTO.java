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

    int age;

    public StockAgeDTO(double quantity, Date entryDate, int age) {
        this.quantity = quantity;
        this.date = entryDate;
        this.age = age;
    }
}
