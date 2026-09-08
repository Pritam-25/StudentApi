package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.maityp394.studentapi.entity.Student;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Request ID Filter & Correlation Integration Tests")
class RequestIdFilterIntegrationTest extends BaseIntegrationTest {

  private static final String REQUEST_ID_HEADER = "X-Request-ID";

  @Test
  @DisplayName("Should generate valid UUID X-Request-ID header when missing in incoming request")
  void shouldGenerateRequestIdHeaderWhenMissingInRequest() {
    Student student = createClassRepresentative();
    String token = createAccessToken(student);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String requestId = response.getHeaders().getFirst(REQUEST_ID_HEADER);
    assertThat(requestId).isNotBlank().satisfies(id -> assertThat(UUID.fromString(id)).isNotNull());
  }

  @Test
  @DisplayName("Should preserve and echo back incoming client-supplied X-Request-ID header")
  void shouldPreserveIncomingRequestIdHeader() {
    Student student = createClassRepresentative();
    String token = createAccessToken(student);

    String customRequestId = "custom-trace-id-12345";
    HttpHeaders headers = createBearerHeaders(token);
    headers.set(REQUEST_ID_HEADER, customRequestId);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst(REQUEST_ID_HEADER)).isEqualTo(customRequestId);
  }

  @Test
  @DisplayName("Should include X-Request-ID header on error/exception responses")
  void shouldIncludeRequestIdHeaderOnExceptionResponses() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);

    String customRequestId = "error-trace-id-999";
    HttpHeaders headers = createBearerHeaders(token);
    headers.set(REQUEST_ID_HEADER, customRequestId);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/invalid-uuid",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getHeaders().getFirst(REQUEST_ID_HEADER)).isEqualTo(customRequestId);
  }

  @Test
  @DisplayName("Should discard invalid or malicious X-Request-ID and generate a clean UUID")
  void shouldDiscardInvalidRequestIdAndGenerateCleanUuid() {
    Student student = createClassRepresentative();
    String token = createAccessToken(student);

    HttpHeaders headers = createBearerHeaders(token);
    headers.set(REQUEST_ID_HEADER, "invalid_id_with_special_chars!@#$%^&*()<>");

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String requestId = response.getHeaders().getFirst(REQUEST_ID_HEADER);
    assertThat(requestId)
        .isNotBlank()
        .isNotEqualTo("invalid_id_with_special_chars!@#$%^&*()<>")
        .satisfies(id -> assertThat(UUID.fromString(id)).isNotNull());
  }
}
