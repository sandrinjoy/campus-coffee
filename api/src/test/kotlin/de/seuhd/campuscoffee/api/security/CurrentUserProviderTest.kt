package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Tests [CurrentUserProvider], the bridge from the Spring Security principal to the domain [User].
 */
class CurrentUserProviderTest {
    private val userService: UserService = mock()
    private val currentUserProvider = CurrentUserProvider(userService)

    @AfterEach
    fun cleanUp() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `currentUser returns resolved domain user when authenticated`() {
        val fixtureUser = TestFixtures.getUserFixtures().first()
        val auth: Authentication = mock()
        whenever(auth.isAuthenticated).thenReturn(true)
        whenever(auth.name).thenReturn(fixtureUser.loginName)

        val context: SecurityContext = mock()
        whenever(context.authentication).thenReturn(auth)
        SecurityContextHolder.setContext(context)

        whenever(userService.getByLoginName(fixtureUser.loginName)).thenReturn(fixtureUser)

        val resolved = currentUserProvider.currentUser()
        assertThat(resolved).isEqualTo(fixtureUser)
    }

    @Test
    fun `currentUser throws when no authenticated context exists`() {
        // No authentication context set
        assertThrows<IllegalArgumentException> { currentUserProvider.currentUser() }

        // Context set but not authenticated
        val auth: Authentication = mock()
        whenever(auth.isAuthenticated).thenReturn(false)
        val context: SecurityContext = mock()
        whenever(context.authentication).thenReturn(auth)
        SecurityContextHolder.setContext(context)

        assertThrows<IllegalArgumentException> { currentUserProvider.currentUser() }
    }
}
