package com.maityp394.studentapi.security.oauth2;

import com.maityp394.studentapi.config.properties.RedisSessionProperties;
import com.maityp394.studentapi.security.session.RedisSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Repository;

/**
 * Stateless Redis-backed implementation of {@link AuthorizationRequestRepository}.
 *
 * <p>Persists {@link OAuth2AuthorizationRequest} instances keyed by their unique {@code state}
 * parameter in Redis, enabling seamless, stateless OAuth2 login flows across distributed
 * application nodes without requiring server-side HTTP sessions.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private final RedisSessionService redisSessionService;
  private final RedisSessionProperties sessionProperties;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(@NonNull HttpServletRequest request) {
    String state = getStateParameter(request);
    if (state == null) {
      return null;
    }
    return redisSessionService.getOAuth2Request(state);
  }

  @Override
  public void saveAuthorizationRequest(
      @NonNull OAuth2AuthorizationRequest authorizationRequest,
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response) {

    String state = authorizationRequest.getState();
    if (state != null) {
      Duration ttl = Duration.ofSeconds(sessionProperties.oauthStateTtlSeconds());
      redisSessionService.saveOAuth2Request(state, authorizationRequest, ttl);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      @NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
    String state = getStateParameter(request);
    if (state == null) {
      return null;
    }
    return redisSessionService.removeOAuth2Request(state);
  }

  private String getStateParameter(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String state = request.getParameter(OAuth2ParameterNames.STATE);
    return (state != null && !state.isBlank()) ? state : null;
  }
}
