package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.model.objects.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService
import de.seuhd.campuscoffee.domain.ports.data.PasswordHasher
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Implementation of the User service that handles business logic related to user entities.
 */
@Service
class UserServiceImpl(
    private val userDataService: UserDataService,
    private val passwordHasher: PasswordHasher
) : CrudServiceImpl<User, Long>(User::class.java),
    UserService {
    override fun dataService(): CrudDataService<User, Long> = userDataService

    /**
     * Normalizes the password before delegating to the generic upsert. A freshly supplied raw password is
     * hashed and the raw value dropped, so the plaintext is never persisted or read back. An update that
     * omits the password keeps the user's existing stored hash. The write-only password is never sent
     * back to be re-submitted, so an omitted one means "unchanged", not "clear it".
     */
    override fun upsert(domainObject: User): User {
        val raw = domainObject.password
        val id = domainObject.id

        val toUpsert =
            when {
                raw != null -> domainObject.copy(passwordHash = passwordHasher.hash(raw), password = null)
                id != null -> domainObject.copy(passwordHash = userDataService.getById(id).passwordHash)
                else -> domainObject.copy()
            }
        return super.upsert(toUpsert)
    }

    override fun upsert(domainObject: User, actingUser: User): User {
        if (domainObject.id != null) {
            if (actingUser.id != domainObject.id && !actingUser.roles.contains(de.seuhd.campuscoffee.domain.model.objects.Role.ADMIN)) {
                throw de.seuhd.campuscoffee.domain.exceptions.ForbiddenException("Cannot edit another user's profile.")
            }
            val existing = getById(domainObject.id)
            if (domainObject.roles != existing.roles && !actingUser.roles.contains(de.seuhd.campuscoffee.domain.model.objects.Role.ADMIN)) {
                throw de.seuhd.campuscoffee.domain.exceptions.ForbiddenException("Cannot change roles.")
            }
        }
        return upsert(domainObject)
    }

    override fun getById(id: Long, actingUser: User): User {
        val target = getById(id)
        if (actingUser.id != target.id && !actingUser.roles.contains(de.seuhd.campuscoffee.domain.model.objects.Role.ADMIN)) {
            throw de.seuhd.campuscoffee.domain.exceptions.ForbiddenException("Cannot read another user's profile.")
        }
        return target
    }

    override fun getByLoginName(loginName: String, actingUser: User): User {
        val target = getByLoginName(loginName)
        if (actingUser.id != target.id && !actingUser.roles.contains(de.seuhd.campuscoffee.domain.model.objects.Role.ADMIN)) {
            throw de.seuhd.campuscoffee.domain.exceptions.ForbiddenException("Cannot read another user's profile.")
        }
        return target
    }

    override fun delete(id: Long, actingUser: User) {
        if (!actingUser.roles.contains(de.seuhd.campuscoffee.domain.model.objects.Role.ADMIN)) {
            throw de.seuhd.campuscoffee.domain.exceptions.ForbiddenException("Only ADMIN can delete users.")
        }
        delete(id)
    }

    override fun getByLoginName(loginName: String): User {
        log.debug("Retrieving user with login name: {}", loginName)
        return userDataService.getByLoginName(loginName)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(UserServiceImpl::class.java)
    }
}
