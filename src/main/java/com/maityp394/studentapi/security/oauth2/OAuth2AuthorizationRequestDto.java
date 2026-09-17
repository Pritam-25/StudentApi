package com.maityp394.studentapi.security.oauth2;

import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Data Transfer Object for serializing and deserializing {@link OAuth2AuthorizationRequest} to/from
 * Redis using JSON.
 *
 * <p>Decouples stateless Redis caching from Spring Security internal class hierarchy and ensures
 * reliable JSON serialization without Java object deserialization risks.
 */
public record OAuth2AuthorizationRequestDto(
    String authorizationUri,
    String clientId,
    String redirectUri,
    Set<String> scopes,
    String state,
    Map<String, Object> additionalParameters,
    String authorizationRequestUri,
    Map<String, Object> attributes) {

  /**
   * Converts a Spring Security {@link OAuth2AuthorizationRequest} to this serializable DTO.
   *
   * @param request the authorization request to convert
   * @return the populated DTO, or {@code null} if request is null
   */
  public static OAuth2AuthorizationRequestDto from(OAuth2AuthorizationRequest request) {
    if (request == null) {
      return null;
    }
    return new OAuth2AuthorizationRequestDto(
        request.getAuthorizationUri(),
        request.getClientId(),
        request.getRedirectUri(),
        request.getScopes(),
        request.getState(),
        request.getAdditionalParameters(),
        request.getAuthorizationRequestUri(),
        request.getAttributes());
  }

  /**
   * Reconstitutes an {@link OAuth2AuthorizationRequest} from this DTO.
   *
   * @return the reconstructed {@link OAuth2AuthorizationRequest}
   */
  public OAuth2AuthorizationRequest toOAuth2AuthorizationRequest() {
    OAuth2AuthorizationRequest.Builder builder =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(this.authorizationUri)
            .clientId(this.clientId)
            .redirectUri(this.redirectUri)
            .scopes(this.scopes)
            .state(this.state)
            .additionalParameters(this.additionalParameters)
            .authorizationRequestUri(this.authorizationRequestUri);

    if (this.attributes != null && !this.attributes.isEmpty()) {
      builder.attributes(
          attrs -> {
            attrs.clear();
            attrs.putAll(this.attributes);
          });
    }

    return builder.build();
  }
}
