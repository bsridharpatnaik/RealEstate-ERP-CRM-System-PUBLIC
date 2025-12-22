package com.ec.application.datasync;

import com.ec.application.model.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultiTenantSyncService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${schemas.list}")
    private String schemasList;

    @Value("${master.schema}")
    private String masterSchema;

    private List<String> targetSchemas;

    public MultiTenantSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Build target schema list once and exclude master schema
     */
    @PostConstruct
    public void init() {
        targetSchemas = Arrays.stream(schemasList.split(","))
                .map(String::trim)
                .filter(s -> !s.equalsIgnoreCase(masterSchema))
                .collect(Collectors.toList());
    }

    @Transactional
    public void syncRow(String tableName,
                        Map<String, Object> rowData,
                        String action,
                        List<String> pkColumns) {

        if (rowData.isEmpty() || pkColumns.isEmpty()) {
            return;
        }

        Map<String, Object> normalizedData = normalizeRowData(tableName, rowData);

        for (String tenant : targetSchemas) {
            if ("INSERT".equalsIgnoreCase(action)) {
                insertRow(tenant, tableName, normalizedData);
            } else if ("UPDATE".equalsIgnoreCase(action)) {
                updateRow(tenant, tableName, normalizedData, pkColumns);
            } else if ("DELETE".equalsIgnoreCase(action)) {
                deleteRow(tenant, tableName, normalizedData, pkColumns);
            }
        }
    }

    private void insertRow(String schema, String table, Map<String, Object> data) {

        String columns = String.join(", ", data.keySet());
        String values = data.keySet().stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format(
                "INSERT INTO %s.%s (%s) VALUES (%s)",
                schema, table, columns, values
        );

        jdbcTemplate.update(sql, data.values().toArray());
    }

    private void updateRow(String schema,
                           String table,
                           Map<String, Object> data,
                           List<String> pkColumns) {

        String setClause = data.keySet().stream()
                .filter(c -> !pkColumns.contains(c))
                .map(c -> c + "=?")
                .collect(Collectors.joining(", "));

        String whereClause = pkColumns.stream()
                .map(pk -> pk + "=?")
                .collect(Collectors.joining(" AND "));

        String sql = String.format(
                "UPDATE %s.%s SET %s WHERE %s",
                schema, table, setClause, whereClause
        );

        List<Object> params = new ArrayList<Object>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!pkColumns.contains(entry.getKey())) {
                params.add(entry.getValue());
            }
        }

        for (String pk : pkColumns) {
            params.add(data.get(pk));
        }

        jdbcTemplate.update(sql, params.toArray());
    }

    private void deleteRow(String schema,
                           String table,
                           Map<String, Object> data,
                           List<String> pkColumns) {

        String whereClause = pkColumns.stream()
                .map(pk -> pk + "=?")
                .collect(Collectors.joining(" AND "));

        String sql = String.format(
                "DELETE FROM %s.%s WHERE %s",
                schema, table, whereClause
        );

        List<Object> params = new ArrayList<Object>();
        for (String pk : pkColumns) {
            params.add(data.get(pk));
        }

        jdbcTemplate.update(sql, params.toArray());
    }

    private Map<String, Object> normalizeRowData(
            String tableName,
            Map<String, Object> rowData) {

        Map<String, Object> normalized = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            String column = entry.getKey();
            Object value = entry.getValue();

            // Skip relation field itself
            if ("category".equalsIgnoreCase(column)) {
                continue;
            }

            // Extract FK
            if (value instanceof Category) {
                Category category = (Category) value;
                normalized.put("categoryId", category.getCategoryId());
                continue;
            }

            // Ignore non-db fields
            if ("serialVersionUID".equals(column)) {
                continue;
            }

            normalized.put(column, value);
        }

        return normalized;
    }
}