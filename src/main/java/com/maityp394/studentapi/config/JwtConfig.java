package com.maityp394.studentapi.config;

import com.maityp394.studentapi.config.properties.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Configuration class responsible for creating and exposing JWT signing and verification beans.
 *
 * <p>Centralizes secret key material so that both the {@link JwtEncoder} and {@link JwtDecoder}
 * share the same {@link SecretKey} instance. The decoder is configured with issuer and timestamp
 * validators to reject forged or expired tokens based on {@link JwtProperties}.
 */
@Configuration
public class JwtConfig {

  private static final String HMAC_SHA_256 = "HmacSHA256";

  /**
   * Creates the shared HMAC secret key from the validated {@link JwtProperties}.
   *
   * <p>Because {@link JwtProperties} enforces {@code @NotBlank} and {@code @Size(min = 32)} at
   * application startup, the secret is guaranteed to provide at least 256 bits of key material.
   *
   * @param jwtProperties the validated JWT configuration properties
   * @return a {@link SecretKey} suitable for HMAC-SHA256 operations
   */
  @Bean
  public SecretKey jwtSigningKey(JwtProperties jwtProperties) {
    return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
  }

  /**
   * Configures a {@link JwtDecoder} bean that verifies incoming JWT tokens using the shared secret
   * key and validates the {@code iss} (issuer) and {@code exp}/{@code nbf} (timestamp) claims.
   *
   * @param jwtSigningKey the shared HMAC secret key
   * @param jwtProperties the validated JWT configuration properties
   * @return the configured {@link JwtDecoder}
   */
  @Bean
  public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties jwtProperties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(), new JwtIssuerValidator(jwtProperties.issuer())));
    return decoder;
  }

  /**
   * Configures a {@link JwtEncoder} bean for creating and signing JWT tokens.
   *
   * @param jwtSigningKey the shared HMAC secret key
   * @return the configured {@link JwtEncoder}
   */
  @Bean
  public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
  }
}
