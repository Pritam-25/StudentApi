package com.maityp394.studentapi.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@DisplayName("OAuth2AuthorizationRequestDto Unit Tests")
class OAuth2AuthorizationRequestDtoTest {

  @Test
  @DisplayName("from and toOAuth2AuthorizationRequest should roundtrip all attributes faithfully")
  void shouldRoundtripAuthorizationRequest() {
    OAuth2AuthorizationRequest original =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("client-id-123")
            .redirectUri("http://localhost:8000/login/oauth2/code/google")
            .scopes(Set.of("openid", "profile", "email"))
            .state("state-xyz-789")
            .additionalParameters(Map.of("prompt", "consent"))
            .authorizationRequestUri(
                "https://accounts.google.com/o/oauth2/v2/auth?client_id=client-id-123")
            .attributes(Map.of("registration_id", "google"))
            .build();

    OAuth2AuthorizationRequestDto dto = OAuth2AuthorizationRequestDto.from(original);
    assertThat(dto).isNotNull();

    OAuth2AuthorizationRequest reconstituted = dto.toOAuth2AuthorizationRequest();
    assertThat(reconstituted).isNotNull();
    assertThat(reconstituted.getAuthorizationUri()).isEqualTo(original.getAuthorizationUri());
    assertThat(reconstituted.getClientId()).isEqualTo(original.getClientId());
    assertThat(reconstituted.getRedirectUri()).isEqualTo(original.getRedirectUri());
    assertThat(reconstituted.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
    assertThat(reconstituted.getState()).isEqualTo("state-xyz-789");
    assertThat(reconstituted.getAdditionalParameters()).containsEntry("prompt", "consent");
    assertThat(reconstituted.getAttributes()).containsEntry("registration_id", "google");
  }

  @Test
  @DisplayName("from should return null when passed null request")
  void shouldReturnNullOnNullInput() {
    assertThat(OAuth2AuthorizationRequestDto.from(null)).isNull();
  }
}
