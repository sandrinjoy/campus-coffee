package de.seuhd.campuscoffee.data.constraints;

import java.util.function.Function;

/**
 * Maps a database unique constraint to the domain field it guards. When the constraint is violated, the
 * value extractor reads the offending value from the domain object for the {@code DuplicationException} message.
 *
 * @param <DOMAIN>       the domain model type that holds the unique field
 * @param valueExtractor reads the unique field's value from the domain object (e.g. {@code Pos::name})
 * @param columnName     the database column of the unique field
 * @param constraintName the name of the unique constraint in the database
 */
public record ConstraintMapping<DOMAIN>(
        Function<DOMAIN, Object> valueExtractor,
        String columnName,
        String constraintName
) {
    public Object extractValue(DOMAIN domain) {
        return valueExtractor.apply(domain);
    }
}
