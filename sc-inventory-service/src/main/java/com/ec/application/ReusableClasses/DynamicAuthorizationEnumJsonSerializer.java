package com.ec.application.ReusableClasses;

import com.ec.application.data.AuthorizationEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class DynamicAuthorizationEnumJsonSerializer extends JsonSerializer<AuthorizationEnum> {

    @Override
    public void serialize(AuthorizationEnum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
