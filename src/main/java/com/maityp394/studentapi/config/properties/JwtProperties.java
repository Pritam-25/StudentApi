package com.maityp394.studentapi.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed, validated configuration properties for JWT token minting and verification.
 *
 * @param issuer the JWT token issuer identifier
 * @param secret the HMAC secret string (must be at least 32 characters)
 * @param expirationMs the access token time-to-live in milliseconds
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
    @NotBlank(message = "JWT issuer must not be blank") @DefaultValue("api.maityp394.live")
        String issuer,
    @NotBlank(message = "JWT secret must not be blank") @Size(min = 32, message = "JWT secret must be at least 32 characters") String secret,
    @Positive(message = "JWT expiration must be greater than 0 ms") @DefaultValue("900000")
        long expirationMs) {}
