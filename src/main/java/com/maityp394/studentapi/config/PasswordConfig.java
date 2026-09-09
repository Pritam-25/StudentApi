package com.maityp394.studentapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class exposing security-related utility beans such as password hashing encoders.
 */
@Configuration
public class PasswordConfig {

  /**
   * Provides a thread-safe {@link PasswordEncoder} bean implementing the BCrypt strong hashing
   * algorithm with salt generation.
   *
   * @return a configured {@link BCryptPasswordEncoder} instance
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
