package com.maityp394.studentapi.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter responsible for generating and propagating a unique Request ID for cross-cutting
 * HTTP request tracing.
 *
 * <p>Executes once per request. Populates the request attribute, response header, and SLF4J MDC
 * diagnostic context key {@code "requestId"}. Ensures MDC cleanup in a {@code finally} block to
 * prevent thread context pollution in servlet container thread pools.
 */
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String REQUEST_ID_ATTRIBUTE = "requestId";
  public static final String MDC_REQUEST_ID = "requestId";

  private static final java.util.regex.Pattern SAFE_REQUEST_ID_PATTERN =
      java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || !SAFE_REQUEST_ID_PATTERN.matcher(requestId.trim()).matches()) {
      requestId = UUID.randomUUID().toString();
    } else {
      requestId = requestId.trim();
    }

    request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    MDC.put(MDC_REQUEST_ID, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_REQUEST_ID);
    }
  }
}
