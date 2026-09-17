package com.maityp394.studentapi.security.oauth2;

import com.maityp394.studentapi.config.properties.OAuth2Properties;
import com.maityp394.studentapi.exception.OAuthAccountLinkingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Authentication failure handler that intercepts OAuth2/OIDC errors, cleans up cached authorization
 * requests, and redirects to the configured frontend error destination with error details.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final OAuth2Properties oAuth2Properties;
  private final AuthorizationRequestRepository<OAuth2AuthorizationRequest>
      authorizationRequestRepository;

  @Override
  public void onAuthenticationFailure(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AuthenticationException exception)
      throws IOException {

    String errorCode = resolveErrorCode(exception);
    log.warn("OAuth2 authentication failed with code '{}': {}", errorCode, exception.getMessage());

    authorizationRequestRepository.removeAuthorizationRequest(request, response);

    String targetUrl =
        UriComponentsBuilder.fromUriString(oAuth2Properties.failureRedirectUri())
            .replaceQueryParam("error", errorCode)
            .build()
            .toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  private String resolveErrorCode(AuthenticationException exception) {
    Throwable current = exception;
    while (current != null) {
      if (current instanceof OAuthAccountLinkingException) {
        return "account_linking_required";
      }
      current = current.getCause();
    }

    if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
      return oauth2Exception.getError().getErrorCode();
    }
    return "oauth_failed";
  }
}
