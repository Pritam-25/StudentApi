package com.maityp394.studentapi.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.config.properties.RedisSessionProperties;
import com.maityp394.studentapi.security.session.RedisSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@DisplayName("RedisOAuth2AuthorizationRequestRepository Unit Tests")
class RedisOAuth2AuthorizationRequestRepositoryTest {

  private RedisSessionService redisSessionService;
  private RedisOAuth2AuthorizationRequestRepository repository;

  @BeforeEach
  void setUp() {
    redisSessionService = mock(RedisSessionService.class);
    RedisSessionProperties sessionProperties = new RedisSessionProperties(604800L, 2592000L, 300L);
    repository =
        new RedisOAuth2AuthorizationRequestRepository(redisSessionService, sessionProperties);
  }

  @Test
  @DisplayName("saveAuthorizationRequest should persist request to Redis when valid")
  void shouldSaveAuthorizationRequest() {
    OAuth2AuthorizationRequest authRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("test-client")
            .redirectUri("http://localhost:8000/login/oauth2/code/google")
            .state("sample-state-123")
            .build();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    repository.saveAuthorizationRequest(authRequest, request, response);

    verify(redisSessionService)
        .saveOAuth2Request("sample-state-123", authRequest, Duration.ofSeconds(300));
  }

  @Test
  @DisplayName("loadAuthorizationRequest should query Redis by state query parameter")
  void shouldLoadAuthorizationRequest() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("state")).thenReturn("state-xyz");

    OAuth2AuthorizationRequest authRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("test-client")
            .redirectUri("http://localhost:8000/login/oauth2/code/google")
            .state("state-xyz")
            .build();

    when(redisSessionService.getOAuth2Request("state-xyz")).thenReturn(authRequest);

    OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

    assertThat(loaded).isNotNull();
    assertThat(loaded.getState()).isEqualTo("state-xyz");
  }

  @Test
  @DisplayName("removeAuthorizationRequest should delete and return cached request")
  void shouldRemoveAuthorizationRequest() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("state")).thenReturn("state-abc");

    OAuth2AuthorizationRequest authRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("test-client")
            .redirectUri("http://localhost:8000/login/oauth2/code/google")
            .state("state-abc")
            .build();

    when(redisSessionService.removeOAuth2Request("state-abc")).thenReturn(authRequest);

    OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

    assertThat(removed).isEqualTo(authRequest);
    verify(redisSessionService).removeOAuth2Request("state-abc");
  }

  @Test
  @DisplayName("loadAuthorizationRequest should return null if state parameter is missing")
  void shouldReturnNullWhenStateIsMissing() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("state")).thenReturn(null);

    OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);
    assertThat(loaded).isNull();
  }
}
