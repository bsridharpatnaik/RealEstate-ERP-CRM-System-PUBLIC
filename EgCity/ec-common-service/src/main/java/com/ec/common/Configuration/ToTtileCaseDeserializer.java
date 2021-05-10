package com.ec.common.Configuration;

import java.io.IOException;

import org.apache.commons.lang.WordUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ToTtileCaseDeserializer extends StdDeserializer<String> {

    private static final long serialVersionUID = 7527542687158493910L;

    public ToTtileCaseDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException 
    {
    	String text = jsonParser.getText();
    	return WordUtils.capitalizeFully(text);
    }

}