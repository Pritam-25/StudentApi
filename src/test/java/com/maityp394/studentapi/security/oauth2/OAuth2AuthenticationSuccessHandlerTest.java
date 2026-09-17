package com.maityp394.studentapi.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.config.properties.OAuth2Properties;
import com.maityp394.studentapi.config.properties.SecurityProperties;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.security.token.AuthTokens;
import com.maityp394.studentapi.security.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@DisplayName("OAuth2AuthenticationSuccessHandler Unit Tests")
class OAuth2AuthenticationSuccessHandlerTest {

  private TokenService tokenService;
  private OAuth2Properties oAuth2Properties;
  private SecurityProperties securityProperties;

  @SuppressWarnings("unchecked")
  private AuthorizationRequestRepository<OAuth2AuthorizationRequest>
      authorizationRequestRepository = mock(AuthorizationRequestRepository.class);

  private OAuth2AuthenticationSuccessHandler handler;

  @BeforeEach
  void setUp() {
    tokenService = mock(TokenService.class);
    oAuth2Properties =
        new OAuth2Properties(
            "http://localhost:3000/oauth/callback",
            "http://localhost:3000/login?error=oauth_failed");
    securityProperties =
        new SecurityProperties(
            new SecurityProperties.CookieProperties(false),
            new SecurityProperties.CorsProperties(List.of("http://localhost:3000")));

    handler =
        new OAuth2AuthenticationSuccessHandler(
            tokenService, oAuth2Properties, securityProperties, authorizationRequestRepository);
  }

  @Test
  @DisplayName("onAuthenticationSuccess should issue tokens, set cookies, and redirect to callback")
  void shouldHandleAuthenticationSuccess() throws IOException {
    UUID studentId = UUID.randomUUID();
    Student student =
        new Student("Pritam", "pritam@example.com", Responsibility.STUDENT, "google-123");
    student.setId(studentId);

    OidcUser delegate = mock(OidcUser.class);
    StudentOidcUser oidcUser = new StudentOidcUser(student, delegate);

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(oidcUser);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("TestAgent/1.0");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getContextPath()).thenReturn("");

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.encodeRedirectURL(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AuthTokens tokens =
        new AuthTokens(
            "access.jwt.token", 900L, "session-123.refresh-secret", 604800L, UUID.randomUUID());
    when(tokenService.issueTokens(student, "TestAgent/1.0", "127.0.0.1")).thenReturn(tokens);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);

    ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
    verify(response, org.mockito.Mockito.atLeastOnce())
        .addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
    List<String> cookies = cookieCaptor.getAllValues();
    assertThat(cookies).anyMatch(c -> c.contains("access_token=access.jwt.token"));
    assertThat(cookies).anyMatch(c -> c.contains("refresh_token=session-123.refresh-secret"));

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).sendRedirect(redirectCaptor.capture());
    assertThat(redirectCaptor.getValue()).isEqualTo("http://localhost:3000/oauth/callback");
  }
}
