package com.maityp394.studentapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

/**
 * Custom request attribute handler tailored for Single Page Applications (SPAs).
 *
 * <p>Preserves BREACH protection via {@link XorCsrfTokenRequestAttributeHandler} when writing
 * attributes while resolving plain string values directly from HTTP request headers (such as {@code
 * X-XSRF-TOKEN}).
 */
public final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

  private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
    this.delegate.handle(request, response, csrfToken);
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    String headerValue = request.getHeader(csrfToken.getHeaderName());
    return (headerValue != null && !headerValue.isBlank())
        ? super.resolveCsrfTokenValue(request, csrfToken)
        : this.delegate.resolveCsrfTokenValue(request, csrfToken);
  }
}
