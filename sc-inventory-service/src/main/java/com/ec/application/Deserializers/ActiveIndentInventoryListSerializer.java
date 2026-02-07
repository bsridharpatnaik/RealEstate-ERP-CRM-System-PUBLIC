package com.ec.application.Deserializers;

import com.ec.application.model.IndentInventoryList;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Set;

public class ActiveIndentInventoryListSerializer
        extends JsonSerializer<Set<IndentInventoryList>> {

    @Override
    public void serialize(Set<IndentInventoryList> value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        for (IndentInventoryList item : value) {
            if (item.isDeleted()) {
                continue; // skip soft-deleted items
            }
            gen.writeObject(item);
        }
        gen.writeEndArray();
    }
}
