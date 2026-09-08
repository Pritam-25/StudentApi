package com.maityp394.studentapi.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed configuration properties for application security settings.
 *
 * @param cookie nested cookie configuration
 * @param cors nested CORS configuration
 */
@ConfigurationProperties(prefix = "app.security")
@Validated
public record SecurityProperties(
    @DefaultValue CookieProperties cookie, @DefaultValue CorsProperties cors) {

  /**
   * Cookie security settings.
   *
   * @param secure whether cookies require HTTPS
   */
  public record CookieProperties(boolean secure) {}

  /**
   * CORS security settings.
   *
   * @param allowedOrigins list of allowed origin URLs
   */
  public record CorsProperties(
      @DefaultValue({"http://localhost:3000"}) List<String> allowedOrigins) {}
}
