package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.mapper.PosDtoMapper
import de.seuhd.campuscoffee.api.mapper.UserDtoMapper
import de.seuhd.campuscoffee.domain.ports.api.PosService
import de.seuhd.campuscoffee.domain.ports.api.ReviewService
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.tests.SystemTestUtils.configureClient
import de.seuhd.campuscoffee.tests.SystemTestUtils.configurePostgresContainers
import de.seuhd.campuscoffee.tests.SystemTestUtils.getPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Shared context for the system test suite: starts the Spring application on a random port and points
 * it at a shared PostgreSQL testcontainer. The database schema is migrated on startup via Flyway.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractSystemTest {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var posService: PosService

    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    protected lateinit var reviewService: ReviewService

    @Autowired
    protected lateinit var posDtoMapper: PosDtoMapper

    @Autowired
    protected lateinit var userDtoMapper: UserDtoMapper

    @BeforeEach
    fun beforeEach() {
        // reviews reference POS and users via foreign keys, so they must be cleared first
        reviewService.clear()
        posService.clear()
        userService.clear()
        configureClient(port)

        // Insert default moderator
        userService.upsert(
            de.seuhd.campuscoffee.domain.model.objects.User(
                loginName = "moderator",
                emailAddress = "mod@campus.de",
                firstName = "Mod",
                lastName = "Erat",
                roles = setOf(de.seuhd.campuscoffee.domain.model.objects.Role.USER, de.seuhd.campuscoffee.domain.model.objects.Role.MODERATOR),
                password = "password123"
            )
        )
        // Insert default admin
        userService.upsert(
            de.seuhd.campuscoffee.domain.model.objects.User(
                loginName = "admin",
                emailAddress = "admin@campus.de",
                firstName = "Ad",
                lastName = "Min",
                roles = setOf(de.seuhd.campuscoffee.domain.model.objects.Role.USER, de.seuhd.campuscoffee.domain.model.objects.Role.ADMIN),
                password = "password123"
            )
        )
        // Reset credentials to default moderator
        de.seuhd.campuscoffee.tests.SystemTestUtils.testCredentials = "moderator" to "password123"
    }

    @AfterEach
    fun afterEach() {
        reviewService.clear()
        posService.clear()
        userService.clear()
    }

    companion object {
        // share one testcontainers instance across all system tests
        private val postgresContainer: PostgreSQLContainer<*> = getPostgresContainer().apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configurePostgresContainers(registry, postgresContainer)
        }
    }
}
