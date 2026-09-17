package com.maityp394.studentapi.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.config.properties.OAuth2Properties;
import com.maityp394.studentapi.exception.OAuthAccountLinkingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@DisplayName("OAuth2AuthenticationFailureHandler Unit Tests")
class OAuth2AuthenticationFailureHandlerTest {

  private OAuth2Properties oAuth2Properties;

  @SuppressWarnings("unchecked")
  private AuthorizationRequestRepository<OAuth2AuthorizationRequest>
      authorizationRequestRepository = mock(AuthorizationRequestRepository.class);

  private OAuth2AuthenticationFailureHandler handler;

  @BeforeEach
  void setUp() {
    oAuth2Properties =
        new OAuth2Properties(
            "http://localhost:3000/oauth/callback",
            "http://localhost:3000/login?error=oauth_failed");
    handler =
        new OAuth2AuthenticationFailureHandler(oAuth2Properties, authorizationRequestRepository);
  }

  @Test
  @DisplayName("onAuthenticationFailure should redirect with account_linking_required on collision")
  void shouldRedirectWithAccountLinkingRequired() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.encodeRedirectURL(any())).thenAnswer(invocation -> invocation.getArgument(0));

    OAuthAccountLinkingException exception =
        new OAuthAccountLinkingException("Account already exists with password");

    handler.onAuthenticationFailure(request, response, exception);

    verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).sendRedirect(redirectCaptor.capture());
    assertThat(redirectCaptor.getValue())
        .isEqualTo("http://localhost:3000/login?error=account_linking_required");
  }

  @Test
  @DisplayName(
      "onAuthenticationFailure should redirect with oauth error code from OAuth2AuthenticationException")
  void shouldRedirectWithOAuthErrorCode() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.encodeRedirectURL(any())).thenAnswer(invocation -> invocation.getArgument(0));

    OAuth2AuthenticationException exception =
        new OAuth2AuthenticationException(new OAuth2Error("unverified_email"));

    handler.onAuthenticationFailure(request, response, exception);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).sendRedirect(redirectCaptor.capture());
    assertThat(redirectCaptor.getValue())
        .isEqualTo("http://localhost:3000/login?error=unverified_email");
  }

  @Test
  @DisplayName(
      "onAuthenticationFailure should fallback to oauth_failed on generic authentication error")
  void shouldFallbackToGenericOAuthFailed() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.encodeRedirectURL(any())).thenAnswer(invocation -> invocation.getArgument(0));

    BadCredentialsException exception = new BadCredentialsException("Failed");

    handler.onAuthenticationFailure(request, response, exception);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).sendRedirect(redirectCaptor.capture());
    assertThat(redirectCaptor.getValue())
        .isEqualTo("http://localhost:3000/login?error=oauth_failed");
  }

  @Test
  @DisplayName(
      "onAuthenticationFailure should traverse cause hierarchy to detect wrapped OAuthAccountLinkingException")
  void shouldDetectWrappedOAuthAccountLinkingException() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.encodeRedirectURL(any())).thenAnswer(invocation -> invocation.getArgument(0));

    OAuthAccountLinkingException linkingException =
        new OAuthAccountLinkingException("Account already exists with password");
    OAuth2AuthenticationException wrappedException =
        new OAuth2AuthenticationException(new OAuth2Error("invalid_request"), linkingException);

    handler.onAuthenticationFailure(request, response, wrappedException);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).sendRedirect(redirectCaptor.capture());
    assertThat(redirectCaptor.getValue())
        .isEqualTo("http://localhost:3000/login?error=account_linking_required");
  }
}
