package com.maityp394.studentapi.security.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that ensures deferred {@link CsrfToken} instances are evaluated on every request.
 *
 * <p>In Spring Security 6+, the {@link CsrfToken} is deferred (loaded lazily via a {@code
 * Supplier}). Invoking {@code csrfToken.getToken()} forces its evaluation so that {@link
 * CookieCsrfTokenRepository} persists the {@code XSRF-TOKEN} cookie into the HTTP response.
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

    if (csrfToken != null) {
      csrfToken.getToken();
    }
    filterChain.doFilter(request, response);
  }
}
