package com.maityp394.studentapi.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Redis session management and refresh token rotation.
 *
 * @param idleTimeoutSeconds Sliding session inactivity timeout in seconds (default 7 days: 604800)
 * @param absoluteLifetimeSeconds Maximum session lifetime in seconds (default 30 days: 2592000)
 */
@ConfigurationProperties(prefix = "app.security.session")
@Validated
public record RedisSessionProperties(
    @Min(value = 60, message = "Idle timeout must be at least 60 seconds") @DefaultValue("604800")
        long idleTimeoutSeconds,
    @Min(value = 60, message = "Absolute lifetime must be at least 60 seconds")
        @DefaultValue("2592000")
        long absoluteLifetimeSeconds) {}
