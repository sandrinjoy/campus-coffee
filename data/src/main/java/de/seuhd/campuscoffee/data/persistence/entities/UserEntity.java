package de.seuhd.campuscoffee.data.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Database entity for a registered user.
 */
@jakarta.persistence.Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserEntity extends Entity {
    public static final String LOGIN_NAME_COLUMN = "login_name";
    public static final String EMAIL_ADDRESS_COLUMN = "email_address";

    /** Names of the unique constraints, declared in the Flyway migration. */
    public static final String LOGIN_NAME_UNIQUE_CONSTRAINT = "uq_users_login_name";
    public static final String EMAIL_ADDRESS_UNIQUE_CONSTRAINT = "uq_users_email_address";

    @Column(name = LOGIN_NAME_COLUMN)
    private String loginName;

    @Column(name = EMAIL_ADDRESS_COLUMN)
    private String emailAddress;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;
}
