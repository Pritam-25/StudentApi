package com.maityp394.studentapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.ProblemDetail;

/**
 * Central factory for constructing standardized RFC 9457 {@link ProblemDetail} instances carrying
 * uniform application extension properties ({@code code} and {@code timestamp}).
 *
 * <p>Used across both Spring MVC controller exception handling and Spring Security filter error
 * handlers to ensure complete JSON contract consistency.
 */
public final class ProblemDetailFactory {

  private ProblemDetailFactory() {
    // Utility class
  }

  /**
   * Constructs a standardized {@link ProblemDetail} payload.
   *
   * @param errorCode the business or security {@link ErrorCode}
   * @param detail optional human-readable message (sanitized for client consumption)
   * @param request the active {@link HttpServletRequest} for populating {@code instance}
   * @return a populated {@link ProblemDetail} instance
   */
  public static ProblemDetail create(
      ErrorCode errorCode, String detail, HttpServletRequest request) {

    ProblemDetail problem = ProblemDetail.forStatus(errorCode.getStatus());
    problem.setTitle(errorCode.getTitle());

    if (detail != null && !detail.isBlank()) {
      problem.setDetail(detail);
    }

    if (request != null) {
      problem.setInstance(URI.create(request.getRequestURI()));
    }

    problem.setProperty("code", errorCode.name());
    problem.setProperty("timestamp", Instant.now());

    return problem;
  }
}
