package com.maityp394.studentapi.security.oauth2;

import com.maityp394.studentapi.config.properties.OAuth2Properties;
import com.maityp394.studentapi.config.properties.SecurityProperties;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.security.SecurityConstants;
import com.maityp394.studentapi.security.token.AuthTokens;
import com.maityp394.studentapi.security.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Authentication success handler that bridges Spring Security OAuth2/OIDC login to the
 * application's unified Redis session and token rotation architecture.
 *
 * <p>Issues a new Redis session, sets secure HttpOnly session cookies, cleans up authorization
 * state, and redirects the browser cleanly to the configured frontend success callback URL with
 * zero tokens exposed in the URL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final TokenService tokenService;
  private final OAuth2Properties oAuth2Properties;
  private final SecurityProperties securityProperties;
  private final AuthorizationRequestRepository<OAuth2AuthorizationRequest>
      authorizationRequestRepository;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      Authentication authentication)
      throws IOException {

    StudentOidcUser oidcUser = (StudentOidcUser) authentication.getPrincipal();
    Student student = Objects.requireNonNull(oidcUser).getStudent();

    String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
    String ipAddress = request.getRemoteAddr();

    AuthTokens tokens = tokenService.issueTokens(student, userAgent, ipAddress);

    addSessionCookies(response, tokens);
    authorizationRequestRepository.removeAuthorizationRequest(request, response);
    clearAuthenticationAttributes(request);

    String targetUrl = oAuth2Properties.authorizedRedirectUri();
    log.info(
        "OAuth2 login successful for student id={}; redirecting cleanly to frontend callback: {}",
        student.getId(),
        targetUrl);
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  private void addSessionCookies(HttpServletResponse response, AuthTokens tokens) {
    boolean secure = securityProperties.cookie().secure();

    ResponseCookie accessTokenCookie =
        ResponseCookie.from(SecurityConstants.ACCESS_TOKEN_COOKIE, tokens.accessToken())
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(tokens.accessTokenExpiresIn())
            .sameSite("Lax")
            .build();

    ResponseCookie refreshTokenCookie =
        ResponseCookie.from(SecurityConstants.REFRESH_TOKEN_COOKIE, tokens.refreshToken())
            .httpOnly(true)
            .secure(secure)
            .path("/api/v1/auth")
            .maxAge(tokens.refreshTokenExpiresIn())
            .sameSite("Lax")
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
  }
}
