package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@DisplayName("OAuth2 Integration Tests (/oauth2/authorization/google)")
class OAuth2IntegrationTest extends BaseIntegrationTest {

  @LocalServerPort private int port;

  @Test
  @DisplayName(
      "GET /oauth2/authorization/google - Should initiate OAuth2 flow with PKCE and save state in Redis")
  void shouldInitiateOAuth2FlowWithPkceAndSaveStateInRedis() {
    // RestTemplate configured to not follow 302 redirects automatically
    HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    RestTemplate noRedirectTemplate = new RestTemplate(new JdkClientHttpRequestFactory(httpClient));

    String url = "http://localhost:" + port + "/oauth2/authorization/google";
    ResponseEntity<Void> response = noRedirectTemplate.getForEntity(url, Void.class);

    // Spring Security issues 302 Found redirect to Google authorization endpoint
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    URI location = response.getHeaders().getLocation();
    assertThat(location).isNotNull();
    assertThat(location.getHost()).isEqualTo("accounts.google.com");
    assertThat(location.getPath()).isEqualTo("/o/oauth2/v2/auth");

    // Parse query parameters
    Map<String, String> queryParams =
        Arrays.stream(location.getRawQuery().split("&"))
            .map(param -> param.split("=", 2))
            .collect(
                Collectors.toMap(
                    parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts ->
                        parts.length > 1
                            ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                            : ""));

    assertThat(queryParams.get("response_type")).isEqualTo("code");
    assertThat(queryParams.get("client_id")).isEqualTo("test-google-client-id");
    assertThat(queryParams.get("scope")).contains("openid", "profile", "email");

    // PKCE verification
    assertThat(queryParams.get("code_challenge")).isNotBlank();
    assertThat(queryParams.get("code_challenge_method")).isEqualTo("S256");

    // State verification
    String state = queryParams.get("state");
    assertThat(state).isNotBlank();

    // Verify state was saved into Redis
    String redisKey = "oauth2:req:" + state;
    assertThat(testRedisConfig.stringRedisTemplate().opsForValue().get(redisKey)).isNotNull();
  }
}
