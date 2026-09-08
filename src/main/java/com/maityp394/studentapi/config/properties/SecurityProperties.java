package com.maityp394.studentapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed configuration properties for application security settings.
 *
 * @param cookie nested cookie configuration
 */
@ConfigurationProperties(prefix = "app.security")
@Validated
public record SecurityProperties(@DefaultValue CookieProperties cookie) {

  /**
   * Cookie security settings.
   *
   * @param secure whether cookies require HTTPS
   */
  public record CookieProperties(boolean secure) {}
}
