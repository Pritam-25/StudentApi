package com.maityp394.studentapi.security;

import com.maityp394.studentapi.config.properties.SecurityProperties;
import jakarta.servlet.Filter;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration configuring stateless session management, OAuth2 resource server
 * JWT validation, dual-mode CSRF protection, and authentication manager.
 *
 * <p>JWT signing and verification beans are defined in {@link
 * com.maityp394.studentapi.config.JwtConfig}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;
  private final SecurityProperties securityProperties;

  public SecurityConfig(
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      SecurityProperties securityProperties) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
    this.securityProperties = securityProperties;
  }

  private static final RequestMatcher LOGOUT_PATH =
      PathPatternRequestMatcher.pathPattern("/api/v1/auth/logout");

  /** Paths that are exempted from CSRF protection (safe endpoints and initial auth). */
  private static final RequestMatcher CSRF_EXEMPT_PATHS =
      new OrRequestMatcher(
          PathPatternRequestMatcher.pathPattern("/"),
          PathPatternRequestMatcher.pathPattern("/error"),
          PathPatternRequestMatcher.pathPattern("/favicon.ico"),
          PathPatternRequestMatcher.pathPattern("/.well-known/**"),
          PathPatternRequestMatcher.pathPattern("/actuator/health"),
          PathPatternRequestMatcher.pathPattern("/actuator/info"),
          PathPatternRequestMatcher.pathPattern("/api/v1/auth/login"),
          PathPatternRequestMatcher.pathPattern("/api/v1/auth/register"));

  /** Paths permitted without authentication (all CSRF-exempt public paths plus logout). */
  private static final RequestMatcher AUTH_PUBLIC_PATHS =
      new OrRequestMatcher(CSRF_EXEMPT_PATHS, LOGOUT_PATH);

  /**
   * Configures the main security filter chain for HTTP requests.
   *
   * <p>Enforces CSRF protection for cookie-based browser sessions while exempting public endpoints
   * and stateless {@code Authorization: Bearer} API requests.
   *
   * @param http the {@link HttpSecurity} builder
   * @return the constructed {@link SecurityFilterChain}
   * @throws BeanInitializationException if an error occurs while building the security filter chain
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) {
    try {
      http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .csrf(
              csrf ->
                  csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                      .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                      .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                      .withObjectPostProcessor(
                          new ObjectPostProcessor<Filter>() {
                            @Override
                            public <O extends Filter> O postProcess(O filter) {
                              if (filter instanceof CsrfFilter csrfFilter) {
                                csrfFilter.setRequireCsrfProtectionMatcher(
                                    requireCsrfProtectionMatcher());
                              }
                              return filter;
                            }
                          }))
          .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
              auth ->
                  auth.requestMatchers(AUTH_PUBLIC_PATHS).permitAll().anyRequest().authenticated())
          .oauth2ResourceServer(
              oauth2 ->
                  oauth2
                      .bearerTokenResolver(bearerTokenResolver())
                      .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                      .authenticationEntryPoint(authenticationEntryPoint)
                      .accessDeniedHandler(accessDeniedHandler))
          .exceptionHandling(
              exceptions ->
                  exceptions
                      .authenticationEntryPoint(authenticationEntryPoint)
                      .accessDeniedHandler(accessDeniedHandler));

      return http.build();
    } catch (Exception ex) {
      throw new BeanInitializationException("Failed to build security filter chain", ex);
    }
  }

  /**
   * Checks whether the given request contains an {@code access_token} cookie with a non-blank
   * value.
   */
  private boolean hasAccessTokenCookie(jakarta.servlet.http.HttpServletRequest request) {
    return SecurityConstants.hasAccessTokenCookie(request);
  }

  /**
   * Constructs the matcher that requires CSRF protection only for state-changing browser requests
   * using cookie authentication, exempting public endpoints and Bearer token requests.
   */
  private RequestMatcher requireCsrfProtectionMatcher() {
    return request -> {
      if (!CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)) {
        return false;
      }
      String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
        return false;
      }
      return !CSRF_EXEMPT_PATHS.matches(request)
          && (!LOGOUT_PATH.matches(request) || hasAccessTokenCookie(request));
    };
  }

  /**
   * Configures a custom {@link BearerTokenResolver} that supports dual token transport:
   * prioritizing the standard {@code Authorization: Bearer} header, with fallback to the {@code
   * access_token} HttpOnly cookie.
   *
   * @return the configured {@link BearerTokenResolver}
   */
  @Bean
  public BearerTokenResolver bearerTokenResolver() {
    DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    return request -> {
      String token = delegate.resolve(request);
      if (token != null) {
        return token;
      }
      return SecurityConstants.getAccessTokenFromCookie(request);
    };
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
   * Configures CORS to allow cross-origin requests from configured origins (e.g. Next.js frontend)
   * with credentials and appropriate allowed/exposed headers.
   *
   * @return the configured {@link CorsConfigurationSource}
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
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

  /**
   * Configures a {@link JwtAuthenticationConverter} using the provided authorities' converter.
   *
   * @param authoritiesConverter the converter used to dynamically resolve authorities
   * @return the configured {@link JwtAuthenticationConverter}
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter(
      Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }
}
