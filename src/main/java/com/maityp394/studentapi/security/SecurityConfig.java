package com.maityp394.studentapi.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration configuring stateless session management, OAuth2 resource server
 * JWT validation, authentication manager, and JWT signing beans.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Configures the main security filter chain for HTTP requests.
   *
   * <p>CSRF protection is disabled because this REST API is completely stateless and uses JWT
   * bearer tokens passed via the {@code Authorization: Bearer} header rather than session cookies.
   * Browsers do not automatically attach bearer tokens to cross-site requests, making CSRF attacks
   * impossible.
   *
   * @param http the {@link HttpSecurity} builder
   * @return the constructed {@link SecurityFilterChain}
   */
  @Bean
  @SuppressWarnings(
      "java:S4502") // Disabling CSRF is safe for stateless REST APIs using JWT bearer tokens
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    try {
      http.csrf(csrf -> csrf.disable())
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
              auth ->
                  auth.requestMatchers(
                          "/api/v1/auth/register",
                          "/api/v1/auth/login",
                          "/api/v1/auth/logout",
                          "/",
                          "/actuator/**")
                      .permitAll()
                      .anyRequest()
                      .authenticated())
          .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

      return http.build();
    } catch (Exception ex) {
      throw new BeanInitializationException("Failed to build security filter chain", ex);
    }
  }

  /**
   * Configures an {@link AuthenticationManager} using {@link DaoAuthenticationProvider} and the
   * custom user details service.
   *
   * @param userDetailsService the service providing user records
   * @param passwordEncoder the password encoder for verifying password hashes
   * @return the configured {@link AuthenticationManager}
   */
  @Bean
  public AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  /**
   * Configures a {@link JwtDecoder} bean for decoding and verifying incoming JWT tokens.
   *
   * @param secret the HMAC secret key string (Base64 or UTF-8)
   * @return the configured {@link JwtDecoder}
   */
  @Bean
  public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
    SecretKey key = createSecretKey(secret);
    return NimbusJwtDecoder.withSecretKey(key).build();
  }

  /**
   * Configures a {@link JwtEncoder} bean for creating and signing JWT tokens.
   *
   * @param secret the HMAC secret key string (Base64 or UTF-8)
   * @return the configured {@link JwtEncoder}
   */
  @Bean
  public JwtEncoder jwtEncoder(@Value("${jwt.secret}") String secret) {
    SecretKey key = createSecretKey(secret);
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  private SecretKey createSecretKey(String secret) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(secret);
      if (keyBytes.length < 32) {
        keyBytes = secret.getBytes(StandardCharsets.UTF_8);
      }
    } catch (IllegalArgumentException _) {
      keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    }
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }
}
