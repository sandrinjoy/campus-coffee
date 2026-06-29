package de.seuhd.campuscoffee.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain

/**
 * Spring Security configuration.
 *
 * The starter ships a deliberately *permissive* chain so every endpoint stays open and the existing
 * open-endpoint tests keep passing: students tighten this into a real chain rather than starting from a
 * blank slate. The supporting beans (password encoder, authentication provider/manager, JSON 401 entry
 * point) and the JWT resource-server wiring are already in place so the authentication and JWT exercises
 * are about *policy*, not plumbing.
 */
@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authenticationEntryPoint: AuthenticationEntryPoint
    ): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize(org.springframework.http.HttpMethod.GET, "/api/pos/**", permitAll)
                authorize(org.springframework.http.HttpMethod.GET, "/api/reviews/**", permitAll)
                authorize(org.springframework.http.HttpMethod.POST, "/api/users", permitAll)
                authorize(org.springframework.http.HttpMethod.GET, "/api/users", hasAuthority("ROLE_ADMIN"))
                
                authorize("/api/api-docs/**", permitAll)
                authorize("/api/swagger-ui/**", permitAll)
                authorize("/api/swagger-ui.html", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/api/dev/**", permitAll)

                authorize(org.springframework.http.HttpMethod.POST, "/api/pos/**", hasAuthority("ROLE_MODERATOR"))
                authorize(org.springframework.http.HttpMethod.PUT, "/api/pos/**", hasAuthority("ROLE_MODERATOR"))
                authorize(org.springframework.http.HttpMethod.DELETE, "/api/pos/**", hasAuthority("ROLE_MODERATOR"))
                authorize(org.springframework.http.HttpMethod.DELETE, "/api/users/{id}", hasAuthority("ROLE_ADMIN"))

                authorize(anyRequest, authenticated)
            }
            // Stateless API: no server-side session; the principal comes from the credentials on each request.
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            // Accept HTTP Basic credentials. Bearer-token (JWT) support is wired below.
            httpBasic { }
            // Bearer-token (JWT) resource server. Harmless under permitAll: a missing or invalid token
            // leaves the request anonymous, which permitAll still allows.
            oauth2ResourceServer { 
                jwt {
                    jwtAuthenticationConverter = jwtAuthenticationConverter()
                } 
            }
            // Render an unauthenticated rejection as the application's JSON ErrorResponse (takes effect
            // once the chain requires authentication).
            exceptionHandling { this.authenticationEntryPoint = authenticationEntryPoint }
        }
        return http.build()
    }

    private fun jwtAuthenticationConverter(): org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter {
        val grantedAuthoritiesConverter = org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter()
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_")
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles")

        val jwtAuthenticationConverter = org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        return jwtAuthenticationConverter
    }

    /** Delegating encoder ({bcrypt} by default); shared with the data layer's hashing semantics. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    /** Authenticates username/password against the [UserDetailsService] using the shared encoder. */
    @Bean
    fun authenticationProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    /** Exposes the [AuthenticationManager] so the token endpoint (Exercise 4) can reuse it. */
    @Bean
    fun authenticationManager(authenticationProvider: DaoAuthenticationProvider): AuthenticationManager =
        AuthenticationManager { authentication -> authenticationProvider.authenticate(authentication) }
}
