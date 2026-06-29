package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.TokenRequestDto
import de.seuhd.campuscoffee.api.dtos.TokenResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Authentication endpoint that exchanges credentials for a stateless JWT bearer token. The path is
 * relative to the resource; the central `/api` base is applied by ApiPathConfig.
 *
 * Skeleton in the starter: the endpoint is reachable but unimplemented and answers 501 Not Implemented.
 * Exercise 4 wires it to the AuthenticationManager and returns a JWT (subject = login name, a `roles`
 * claim, a 15-minute expiry) built with the provided JwtEncoder.
 */
@Tag(name = "Authentication", description = "Exchange credentials for a stateless JWT bearer token.")
@Controller
@RequestMapping("/auth")
class AuthController(
    private val authenticationManager: org.springframework.security.authentication.AuthenticationManager,
    private val jwtEncoder: org.springframework.security.oauth2.jwt.JwtEncoder
) {
    @Operation(summary = "Authenticate and return a JWT bearer token.")
    @PostMapping("/token")
    fun token(
        @RequestBody
        @Valid request: TokenRequestDto
    ): ResponseEntity<TokenResponseDto> {
        log.info("Token requested for login name '{}'.", request.loginName)
        
        val authentication = authenticationManager.authenticate(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken(request.loginName, request.password)
        )
        
        val now = java.time.Instant.now()
        val expiry = now.plusSeconds(15 * 60) // 15 minutes
        
        val roles = authentication.authorities.mapNotNull { it.authority?.removePrefix("ROLE_") }
        
        val claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .subject(authentication.name)
            .claim("roles", roles)
            .issuedAt(now)
            .expiresAt(expiry)
            .build()
            
        val headers = org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build()
        val parameters = org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(headers, claims)
        val token = jwtEncoder.encode(parameters).tokenValue
        
        return ResponseEntity.ok(TokenResponseDto(token))
    }

    private companion object {
        private val log = LoggerFactory.getLogger(AuthController::class.java)
    }
}
