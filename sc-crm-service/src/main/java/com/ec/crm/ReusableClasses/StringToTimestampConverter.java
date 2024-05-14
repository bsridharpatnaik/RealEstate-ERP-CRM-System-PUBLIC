package com.ec.crm.ReusableClasses;

import org.springframework.core.convert.converter.Converter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringToTimestampConverter implements Converter<String, Timestamp> {
    @Override
    public Timestamp convert(String source) {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        try {
            Date parsedDate = dateFormat.parse(source);
            return new Timestamp(parsedDate.getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please provide the date in dd-MM-yyyy format.");
        }
    }
}
