package com.maityp394.studentapi.config;

import com.maityp394.studentapi.config.properties.SecurityProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** CORS configuration for cross-origin frontend requests. */
@Configuration
public class CorsConfig {

  private final SecurityProperties securityProperties;

  /**
   * Constructs the CORS configuration with injected security properties.
   *
   * @param securityProperties externalized security properties containing allowed origins
   */
  public CorsConfig(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  /**
   * Creates and registers the global {@link CorsConfigurationSource} bean.
   *
   * @return the configured {@link UrlBasedCorsConfigurationSource} mapped to all paths
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(securityProperties.cors().allowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            HttpHeaders.ORIGIN,
            "X-XSRF-TOKEN",
            "X-Request-ID",
            "X-Requested-With"));
    config.setExposedHeaders(List.of("X-Request-ID"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
