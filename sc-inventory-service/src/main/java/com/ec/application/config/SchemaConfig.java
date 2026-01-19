package com.ec.application.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Getter
@Setter
public class SchemaConfig {

    @Value("${schemas.map}")
    private String schemasMapString;

    @Value("${master.schema}")
    private String masterSchema;

    private Map<String, String> schemaMap = new HashMap<>();
    private List<String> schemaList = new ArrayList<>();
    private List<String> nonMasterSchemaList = new ArrayList<>();


    public SchemaConfig() {
    }

    public SchemaConfig(String schemasMapString) {
        this.schemasMapString = schemasMapString;
    }

    @PostConstruct
    public void init() {
        if (schemasMapString != null && !schemasMapString.trim().isEmpty()) {
            schemaMap = Arrays.stream(schemasMapString.split(","))
                    .map(String::trim)
                    .filter(s -> s.contains(":"))
                    .map(s -> s.split(":", 2))
                    .collect(Collectors.toMap(a -> a[0], a -> a[1]));

            schemaList = new ArrayList<>(schemaMap.keySet());
            nonMasterSchemaList = schemaList.stream().filter(s -> !s.equals(masterSchema)).collect(Collectors.toList());
        }
    }

    public String getSchemaCode(String schemaName) {
        return schemaMap.get(schemaName);
    }

    public boolean isValidSchema(String schema) {
        return schemaMap.containsKey(schema);
    }
}