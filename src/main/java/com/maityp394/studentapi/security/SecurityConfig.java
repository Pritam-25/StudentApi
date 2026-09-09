package com.maityp394.studentapi.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
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

  private static final RequestMatcher PUBLIC_PATHS =
      new OrRequestMatcher(CSRF_EXEMPT_PATHS, LOGOUT_PATH);

  /**
   * Configures the main HTTP security filter chain.
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
   * OAuth2ResourceServerConfigurer auto-adds BearerTokenRequestMatcher to CsrfConfigurer's ignored
   * list. Because our BearerTokenResolver also resolves tokens from the access_token cookie, the
   * configurer API (.requireCsrfProtectionMatcher) would wrap our matcher with AND(ours,
   * NOT(BearerTokenRequestMatcher)), disabling CSRF for cookie-authenticated requests. Setting the
   * matcher directly on CsrfFilter via postProcess bypasses this.
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
   * Dual-mode Bearer token resolver.
   *
   * <p>Checks the standard {@code Authorization: Bearer <token>} header first. If absent, falls
   * back to resolving the token from the {@code access_token} HTTP cookie.
   */
  @Bean
  BearerTokenResolver bearerTokenResolver() {
    DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    return request -> {
      String token = delegate.resolve(request);
      return token != null ? token : SecurityConstants.getAccessTokenFromCookie(request);
    };
  }

  /**
   * Configures the {@link AuthenticationManager} for username/password authentication (e.g.,
   * login).
   *
   * <p>Uses a {@link DaoAuthenticationProvider} wired with the application's {@link
   * UserDetailsService} and {@link PasswordEncoder}.
   */
  @Bean
  AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  /**
   * Configures the {@link JwtAuthenticationConverter} used by the OAuth2 resource server.
   *
   * <p>Applies the custom {@link Converter} to extract roles and authorities from decoded JWT
   * claims into Spring Security {@link GrantedAuthority} collections.
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter(
      Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }
}
