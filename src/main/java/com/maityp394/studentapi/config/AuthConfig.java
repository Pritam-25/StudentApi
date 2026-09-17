package com.maityp394.studentapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Configuration class providing the central {@link AuthenticationManager} bean. */
@Configuration
public class AuthConfig {

  /**
   * Configures the DAO-based authentication manager for username/password authentication.
   *
   * @param userDetailsService user lookup service
   * @param passwordEncoder BCrypt password encoder
   * @return configured {@link AuthenticationManager}
   */
  @Bean
  AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  /**
   * Pre-loads and caches the compiled Redis Lua script for atomic refresh token rotation.
   *
   * @return compiled {@link RedisScript} executing with SHA-1 digest caching
   */
  @Bean
  RedisScript<Long> rotateRefreshTokenScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource("redis/scripts/rotate_refresh_token.lua"));
    script.setResultType(Long.class);
    return script;
  }
}
