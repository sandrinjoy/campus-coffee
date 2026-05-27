package de.seuhd.campuscoffee.data.util;

import jakarta.persistence.Table;

/**
 * Utility class for JPA-related functionality.
 */
public class JpaUtils {
    /**
     * Extracts the table name from the entity's @Table annotation.
     */
    public static String extractTableNameFromEntity(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException(String.format("%s is not annotated with @Table",
                    entityClass.getSimpleName())
            );
        }

        Table table = entityClass.getAnnotation(Table.class);
        String tableName = table.name();
        if (tableName.isEmpty()) {
            throw new IllegalArgumentException("@Table annotation must specify a table name.");
        }

        return tableName;
    }
}
