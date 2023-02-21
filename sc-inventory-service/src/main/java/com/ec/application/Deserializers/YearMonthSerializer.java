package com.ec.application.Deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.regex.Pattern;

public class YearMonthSerializer extends StdDeserializer<String> {
    public YearMonthSerializer() {
        super(String.class);
    }

    @SneakyThrows
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        String value = p.readValueAs(String.class);
        try {

            boolean isMatch = Pattern.compile("^20[0-2][0-9]-((0[1-9])|(1[0-2]))$").matcher(value).matches();
            if (!isMatch)
                throw new Exception("Invalid Date!");
            else return value;
        } catch (Exception e) {
            throw new Exception("Invalid Date!");
        }
    }
}