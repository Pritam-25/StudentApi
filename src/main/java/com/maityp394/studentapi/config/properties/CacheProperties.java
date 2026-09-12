package com.maityp394.studentapi.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed configuration properties for application caching.
 *
 * @param timeToLive default time-to-live duration for cache entries
 */
@ConfigurationProperties(prefix = "app.cache")
@Validated
public record CacheProperties(@DefaultValue("15m") Duration timeToLive) {}
