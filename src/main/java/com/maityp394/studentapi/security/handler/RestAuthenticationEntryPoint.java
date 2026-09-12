package com.maityp394.studentapi.security.handler;

import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.ProblemDetailFactory;
import com.maityp394.studentapi.security.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Authentication entry point that handles unauthenticated and invalid token requests in the
 * security filter chain.
 *
 * <p>Produces standardized RFC 9457 {@link ProblemDetail} JSON payloads directly via {@link
 * ObjectMapper} while preserving standard RFC 6750 {@code WWW-Authenticate: Bearer} challenge
 * headers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final String INVALID_TOKEN_CHALLENGE =
      "Bearer error=\"invalid_token\", error_description=\"The access token is invalid or expired\"";
  private static final String DEFAULT_BEARER_CHALLENGE = "Bearer";

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    log.debug("Authentication failure: {}", authException.getMessage());

    boolean isInvalidToken = isInvalidBearerToken(request, authException);

    ErrorCode errorCode = isInvalidToken ? ErrorCode.INVALID_TOKEN : ErrorCode.UNAUTHORIZED;
    String detail =
        isInvalidToken
            ? "The access token is invalid or expired"
            : "Authentication is required to access this resource";
    String challenge = isInvalidToken ? INVALID_TOKEN_CHALLENGE : DEFAULT_BEARER_CHALLENGE;

    ProblemDetail problem = ProblemDetailFactory.create(errorCode, detail, request);

    response.setStatus(problem.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);

    this.objectMapper.writeValue(response.getOutputStream(), problem);
  }

  /**
   * Determines whether the failure was caused by an invalid/expired token rather than a complete
   * absence of credentials.
   */
  private boolean isInvalidBearerToken(
      HttpServletRequest request, AuthenticationException authException) {

    if (authException instanceof OAuth2AuthenticationException oauth2Ex) {
      OAuth2Error error = oauth2Ex.getError();
      if (error instanceof BearerTokenError bearerTokenError) {
        return "invalid_token".equalsIgnoreCase(bearerTokenError.getErrorCode());
      }
      return "invalid_token".equalsIgnoreCase(error.getErrorCode());
    }

    // Check if the client attempted to supply a token via Bearer header or cookie
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
      return true;
    }

    return SecurityConstants.hasAccessTokenCookie(request);
  }
}
