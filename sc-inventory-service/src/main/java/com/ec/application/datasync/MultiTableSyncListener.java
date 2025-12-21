package com.ec.application.datasync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@Component
public class MultiTableSyncListener {

    @Autowired
    private MultiTenantSyncService syncService;

    @PostPersist
    public void afterInsert(Object entity) {
        sync(entity, "INSERT");
    }

    @PostUpdate
    public void afterUpdate(Object entity) {
        sync(entity, "UPDATE");
    }

    @PostRemove
    public void afterDelete(Object entity) {
        sync(entity, "DELETE");
    }

    private void sync(Object entity, String action) {

        Class<?> clazz = entity.getClass();

        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            return;
        }

        String tableName = table.name();

        Map<String, Object> rowData = extractColumnValues(entity);
        List<String> pkColumns = extractPrimaryKeys(clazz);

        syncService.syncRow(tableName, rowData, action, pkColumns);
    }

    /**
     * Extract only real JPA columns (no serialVersionUID etc.)
     */
    private Map<String, Object> extractColumnValues(Object entity) {

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        Class<?> clazz = entity.getClass();

        while (clazz != null && clazz != Object.class) {

            for (Field field : clazz.getDeclaredFields()) {

                if (Modifier.isStatic(field.getModifiers()) ||
                        Modifier.isTransient(field.getModifiers()) ||
                        field.isAnnotationPresent(Transient.class)) {
                    continue;
                }

                boolean isId = field.isAnnotationPresent(Id.class);
                boolean isColumn = field.isAnnotationPresent(Column.class);

                if (!isId && !isColumn) {
                    continue;
                }

                field.setAccessible(true);

                String columnName;
                if (isColumn) {
                    columnName = field.getAnnotation(Column.class).name();
                } else {
                    columnName = field.getName();
                }

                try {
                    data.put(columnName, field.get(entity));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            clazz = clazz.getSuperclass();
        }

        return data;
    }

    /**
     * Extract PK column(s)
     */
    private List<String> extractPrimaryKeys(Class<?> clazz) {

        List<String> pks = new ArrayList<String>();

        while (clazz != null && clazz != Object.class) {

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    if (field.isAnnotationPresent(Column.class)) {
                        pks.add(field.getAnnotation(Column.class).name());
                    } else {
                        pks.add(field.getName());
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }

        if (pks.isEmpty()) {
            throw new IllegalStateException("No @Id found for " + clazz.getSimpleName());
        }

        return pks;
    }
}
