package com.maityp394.studentapi.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter responsible for logging HTTP request execution summary including HTTP method, URI,
 * query parameters, response status code, and total latency in milliseconds.
 *
 * <p>Emits a clean log message using standard SLF4J. Presentation formatting (colors, padding,
 * profile adaptation) is completely decoupled and handled by the logging layer (Logback).
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long startNanos = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
      log.info(
          "{} {} -> {} ({} ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
    }
  }
}
