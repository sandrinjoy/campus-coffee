package de.seuhd.campuscoffee.data.persistence.entities;

import de.seuhd.campuscoffee.domain.model.enums.CampusType;
import de.seuhd.campuscoffee.domain.model.enums.PosType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Database entity for a point-of-sale (POS).
 */
@jakarta.persistence.Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pos")
public class PosEntity extends Entity {
    public static final String NAME_COLUMN = "name";

    /** Name of the unique constraint on {@code name}, declared in the Flyway migration. */
    public static final String NAME_UNIQUE_CONSTRAINT = "uq_pos_name";

    @Column(name = NAME_COLUMN)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private PosType type;

    @Enumerated(EnumType.STRING)
    private CampusType campus;

    @Embedded
    private AddressEntity address;
}
