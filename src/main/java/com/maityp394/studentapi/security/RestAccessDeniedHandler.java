package com.maityp394.studentapi.security;

import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Access denied handler that handles authorization and CSRF validation failures in the security
 * filter chain.
 *
 * <p>Produces standardized RFC 9457 {@link ProblemDetail} JSON payloads directly via {@link
 * ObjectMapper}.
 */
@Slf4j
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {

    log.debug(
        "Access denied on [{}]: {}", request.getRequestURI(), accessDeniedException.getMessage());

    ErrorCode errorCode;
    String detail;

    if (accessDeniedException instanceof CsrfException) {
      errorCode = ErrorCode.CSRF_INVALID;
      detail = "Invalid or missing CSRF token";
    } else {
      errorCode = ErrorCode.FORBIDDEN;
      detail = "You do not have permission to access this resource";
    }

    ProblemDetail problem = ProblemDetailFactory.create(errorCode, detail, request);

    response.setStatus(problem.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    this.objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
