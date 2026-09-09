package com.maityp394.studentapi.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Main Spring Security configuration for the REST API.
 *
 * <p>Configures:
 *
 * <ul>
 *   <li>Stateless JWT authentication (OAuth2 Resource Server) supporting both headers and cookies.
 *   <li>Dual-mode CSRF protection with SPA token handling.
 *   <li>Database-backed username/password authentication via {@link AuthenticationManager}.
 *   <li>RESTful exception handlers for 401 Unauthorized and 403 Forbidden responses.
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  private static RequestMatcher path(String pattern) {
    return PathPatternRequestMatcher.pathPattern(pattern);
  }

  private static final RequestMatcher LOGOUT_PATH = path("/api/v1/auth/logout");

  private static final RequestMatcher CSRF_EXEMPT_PATHS =
      new OrRequestMatcher(
          path("/"),
          path("/error"),
          path("/favicon.ico"),
          path("/.well-known/**"),
          path("/actuator/health"),
          path("/actuator/info"),
          path("/api/v1/auth/login"),
          path("/api/v1/auth/register"));

  private static final RequestMatcher OPENAPI_DOCS_PATHS =
      new OrRequestMatcher(path("/scalar/**"), path("/v3/api-docs/**"), path("/v3/api-docs.yaml"));

  private static final RequestMatcher PUBLIC_PATHS =
      new OrRequestMatcher(CSRF_EXEMPT_PATHS, LOGOUT_PATH, OPENAPI_DOCS_PATHS);

  /**
   * Configures the primary HTTP security filter chain that protects all incoming API requests.
   *
   * <p>Security layers configured:
   *
   * <ul>
   *   <li><b>CORS:</b> Uses application-defined CorsConfigurationSource defaults.
   *   <li><b>CSRF:</b> Cookie-based (CookieCsrfTokenRepository) with SPA support and custom
   *       matcher.
   *   <li><b>Session:</b> Stateless (no HTTP sessions created or used).
   *   <li><b>Authorization:</b> Permits public and auth endpoints; requires authentication for all
   *       others.
   *   <li><b>OAuth2 Resource Server:</b> Validates JWTs resolved from Authorization header or
   *       cookie.
   *   <li><b>Exception Handling:</b> RESTful 401 Unauthorized and 403 Forbidden JSON responses.
   * </ul>
   *
   * @param http the HTTP security builder to configure
   * @param jwtAuthenticationConverter converter to extract user authorities from JWTs
   * @return the built security filter chain
   */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) {
    http.cors(Customizer.withDefaults())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                    .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                    .withObjectPostProcessor(csrfFilterPostProcessor()))
        .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
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
  }

  /**
   * Customizes the CSRF filter to enforce protection specifically for cookie-based authentication.
   *
   * <p>Why this post-processor is required:
   *
   * <ul>
   *   <li><b>Header Safety:</b> Requests using the {@code Authorization: Bearer} header cannot be
   *       forged across sites by browsers and therefore do not require CSRF protection.
   *   <li><b>Cookie Vulnerability:</b> Requests authenticating via the {@code access_token} cookie
   *       ARE vulnerable to CSRF because browsers automatically attach cookies to cross-site
   *       requests.
   *   <li><b>Spring Security Bypass:</b> By default, Spring's OAuth2 Resource Server automatically
   *       disables CSRF protection when a bearer token resolver is registered. This post-processor
   *       directly configures the CSRF filter to keep cookie-authenticated requests protected.
   * </ul>
   *
   * @return an object post-processor applying the custom CSRF matcher
   */
  private ObjectPostProcessor<OncePerRequestFilter> csrfFilterPostProcessor() {
    return new ObjectPostProcessor<>() {
      @Override
      public <O extends OncePerRequestFilter> O postProcess(O filter) {
        if (filter instanceof CsrfFilter csrfFilter) {
          csrfFilter.setRequireCsrfProtectionMatcher(requireCsrfProtectionMatcher());
        }
        return filter;
      }
    };
  }

  private RequestMatcher requireCsrfProtectionMatcher() {
    return request ->
        CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)
            && !hasBearerToken(request)
            && !CSRF_EXEMPT_PATHS.matches(request)
            && (!LOGOUT_PATH.matches(request) || SecurityConstants.hasAccessTokenCookie(request));
  }

  private boolean hasBearerToken(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    return authorization != null && authorization.startsWith(SecurityConstants.BEARER_PREFIX);
  }

  /**
   * Resolves the bearer token from the {@code Authorization} header, falling back to the {@code
   * access_token} cookie if the header is absent or empty.
   *
   * <p>If token resolution encounters an {@link OAuth2AuthenticationException} on a public
   * endpoint, the exception is suppressed so unauthenticated requests can proceed.
   *
   * @return a {@link BearerTokenResolver} supporting both header and cookie token extraction
   */
  @Bean
  BearerTokenResolver bearerTokenResolver() {
    DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    return request -> {
      String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (authHeader != null
          && authHeader.regionMatches(true, 0, "Bearer", 0, 6)
          && authHeader.substring(6).trim().isEmpty()) {
        return SecurityConstants.getAccessTokenFromCookie(request);
      }
      try {
        String token = delegate.resolve(request);
        return token != null ? token : SecurityConstants.getAccessTokenFromCookie(request);
      } catch (OAuth2AuthenticationException ex) {
        if (PUBLIC_PATHS.matches(request)) {
          return null;
        }
        throw ex;
      }
    };
  }

  /**
   * Configures the central authentication manager responsible for verifying user login credentials.
   *
   * <p>Authentication process:
   *
   * <ul>
   *   <li><b>User Lookup:</b> Retrieves user account details and the stored password hash from the
   *       database using the configured user details service.
   *   <li><b>Password Verification:</b> Compares the raw password supplied during login against the
   *       stored hash using the password encoder (e.g., BCrypt).
   *   <li><b>Authentication Result:</b> Produces a fully authenticated token on success, or throws
   *       an authentication exception if the username or password is invalid.
   * </ul>
   *
   * @param userDetailsService service used to load user accounts from the database
   * @param passwordEncoder the encoder used to verify password hashes
   * @return the configured authentication manager
   */
  @Bean
  AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  /**
   * Configures how claims inside a verified JWT are mapped into Spring Security user permissions.
   *
   * <p>Authority conversion behavior:
   *
   * <ul>
   *   <li><b>Custom Claim Extraction:</b> Reads user roles directly from the {@code authorities}
   *       claim array inside the JWT rather than the default {@code scope} claim.
   *   <li><b>No Prefix Modification:</b> Clears the default {@code SCOPE_} prefix so role names
   *       like {@code ROLE_STUDENT} and {@code ROLE_ADMIN} are preserved exactly as defined.
   *   <li><b>Method Security Support:</b> Allows security annotations such as
   *       {@code @PreAuthorize("hasRole('ADMIN')")} to evaluate role checks without naming
   *       conflicts.
   * </ul>
   *
   * @return the configured JWT authentication converter
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("authorities");
    authoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }
}
