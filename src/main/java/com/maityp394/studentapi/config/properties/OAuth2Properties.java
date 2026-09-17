package com.maityp394.studentapi.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed configuration properties for OAuth2 client redirection flows.
 *
 * @param authorizedRedirectUri Frontend callback URL to redirect to upon successful OAuth
 *     authentication
 * @param failureRedirectUri Frontend login/error URL to redirect to upon OAuth failure
 */
@ConfigurationProperties(prefix = "app.security.oauth2")
@Validated
public record OAuth2Properties(
    @NotBlank @DefaultValue("http://localhost:3000/oauth/callback") String authorizedRedirectUri,
    @NotBlank @DefaultValue("http://localhost:3000/login?error=oauth_failed")
        String failureRedirectUri) {}
