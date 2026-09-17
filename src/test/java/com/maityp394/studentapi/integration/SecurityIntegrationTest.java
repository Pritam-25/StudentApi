package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.entity.Student;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Security & CSRF Integration Tests")
class SecurityIntegrationTest extends BaseIntegrationTest {

  @Test
  @DisplayName("GET /students - Should return 401 UNAUTHORIZED when no credentials provided")
  void shouldRejectUnauthenticatedAccessToProtectedEndpoint() {
    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("UNAUTHORIZED");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Unauthorized");
    assertThat((String) documentContext.read("$.detail"))
        .isEqualTo("Authentication is required to access this resource");
  }

  @Test
  @DisplayName(
      "GET /students - Should return 401 INVALID_TOKEN when Bearer token is invalid or expired")
  void shouldRejectInvalidJwtOnProtectedEndpoint() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("invalid.jwt.token");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/students", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
        .contains("error=\"invalid_token\"");
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("INVALID_TOKEN");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Unauthorized");
    assertThat((String) documentContext.read("$.detail"))
        .isEqualTo("The access token is invalid or expired");
  }

  @Test
  @DisplayName(
      "GET /students - Should authenticate successfully using HttpOnly cookie without Bearer header")
  void shouldAuthenticateUsingHttpOnlyCookieWithoutAuthorizationHeader() {
    Student student = createClassRepresentative();
    String token = createAccessToken(student);
    HttpHeaders headers = createCookieHeaders(token, null);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("GET /students - Should prioritize Authorization Bearer header over cookie token")
  void shouldPrioritizeAuthorizationHeaderOverCookie() {
    Student student = createClassRepresentative();
    String token = createAccessToken(student);

    HttpHeaders headers = createBearerHeaders(token);
    headers.add(HttpHeaders.COOKIE, "access_token=invalid.cookie.token");

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName(
      "PUT /students/{id} - Should reject state-changing cookie request without CSRF token with 403 CSRF_INVALID")
  void shouldRejectStateChangingCookieRequestWithoutCsrfToken() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);

    HttpHeaders headers = createCookieHeaders(token, null);
    UpdateStudentRequest update = new UpdateStudentRequest("Name", "newemail@example.com");
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(update, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + student.getId(), HttpMethod.PUT, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("CSRF_INVALID");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Forbidden");
    assertThat((String) documentContext.read("$.detail"))
        .isEqualTo("Invalid or missing CSRF token");
  }

  @Test
  @DisplayName(
      "PUT /students/{id} - Should accept state-changing Bearer request without CSRF token")
  void shouldAcceptStateChangingBearerRequestWithoutCsrfToken() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);

    HttpHeaders headers = createBearerHeaders(token);
    UpdateStudentRequest update =
        new UpdateStudentRequest("Bearer User", "bearer.update@example.com");
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(update, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + student.getId(), HttpMethod.PUT, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName(
      "PUT /students/{id} - Should accept state-changing cookie request when valid CSRF token is provided")
  void shouldAcceptStateChangingCookieRequestWithCsrfToken() {
    Student student = createClassRepresentative();
    String token = createAccessToken(student);

    // Initial GET request to obtain the XSRF-TOKEN cookie
    HttpHeaders getHeaders = createCookieHeaders(token, null);
    ResponseEntity<String> getResponse =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(getHeaders), String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    List<String> cookies = getResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    String xsrfToken = null;
    if (cookies != null) {
      for (String c : cookies) {
        if (c.startsWith("XSRF-TOKEN=")) {
          int end = c.indexOf(';');
          xsrfToken =
              end != -1
                  ? c.substring("XSRF-TOKEN=".length(), end)
                  : c.substring("XSRF-TOKEN=".length());
          break;
        }
      }
    }
    assertThat(xsrfToken).isNotNull();

    // State-changing request supplying both the cookie and the matching
    // X-XSRF-TOKEN header
    HttpHeaders putHeaders = createCookieHeaders(token, xsrfToken);
    UpdateStudentRequest update =
        new UpdateStudentRequest("Updated Auth User", "auth.user@example.com");
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(update, putHeaders);

    ResponseEntity<String> putResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + student.getId(), HttpMethod.PUT, entity, String.class);

    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName(
      "POST /auth/logout - Should reject state-changing cookie logout without CSRF token with 403 CSRF_INVALID")
  void shouldRejectCookieLogoutWithoutCsrfToken() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);

    HttpHeaders headers = createCookieHeaders(token, null);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/auth/logout", HttpMethod.POST, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("CSRF_INVALID");
  }

  @Test
  @DisplayName(
      "POST /auth/logout - Should accept state-changing cookie logout when valid CSRF token is provided")
  void shouldAcceptCookieLogoutWithCsrfToken() {
    Student student = createClassRepresentative();
    String token = createAccessToken(student);

    // Initial GET request to obtain the XSRF-TOKEN cookie
    HttpHeaders getHeaders = createCookieHeaders(token, null);
    ResponseEntity<String> getResponse =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(getHeaders), String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    List<String> cookies = getResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    String xsrfToken = null;
    if (cookies != null) {
      for (String c : cookies) {
        if (c.startsWith("XSRF-TOKEN=")) {
          int end = c.indexOf(';');
          xsrfToken =
              end != -1
                  ? c.substring("XSRF-TOKEN=".length(), end)
                  : c.substring("XSRF-TOKEN=".length());
          break;
        }
      }
    }
    assertThat(xsrfToken).isNotNull();

    HttpHeaders logoutHeaders = createCookieHeaders(token, xsrfToken);
    HttpEntity<Void> entity = new HttpEntity<>(logoutHeaders);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/auth/logout", HttpMethod.POST, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName(
      "POST /auth/logout - Should require CSRF protection when Authorization header is whitespace Bearer and access_token cookie is present")
  void shouldRejectCookieLogoutWhenAuthorizationHeaderIsWhitespaceBearerWithoutCsrfToken() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);

    HttpHeaders headers = createCookieHeaders(token, null);
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer    ");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/auth/logout", HttpMethod.POST, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("CSRF_INVALID");
  }

  @Test
  @DisplayName(
      "POST /auth/logout - Should exempt valid Bearer token authorization from CSRF without cookie")
  void shouldExemptValidBearerTokenFromCsrfProtection() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);

    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/auth/logout", HttpMethod.POST, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName(
      "GET /auth/me - Should retain existing XSRF-TOKEN cookie without clearing it (Max-Age=0 / Expires=1970)")
  void shouldNotClearCsrfTokenOnAuthenticatedRequestWithExistingCsrfCookie() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);
    String existingXsrfToken = "my-existing-xsrf-token";

    HttpHeaders headers = createCookieHeaders(token, existingXsrfToken);
    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    if (setCookies != null) {
      assertThat(setCookies)
          .noneMatch(
              c -> c.startsWith("XSRF-TOKEN=") && (c.contains("1970") || c.contains("Max-Age=0")));
    }
  }

  @Test
  @DisplayName(
      "OPTIONS /api/v1/students - Should allow CORS preflight from http://localhost:3000 with credentials")
  void shouldAllowCorsPreflightFromAllowedOrigin() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.ORIGIN, "http://localhost:3000");
    headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");
    headers.set(
        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, X-XSRF-TOKEN, X-Request-ID");

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<Void> response =
        testRestTemplate.exchange("/api/v1/students", HttpMethod.OPTIONS, entity, Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getAccessControlAllowOrigin())
        .isEqualTo("http://localhost:3000");
    assertThat(response.getHeaders().getAccessControlAllowCredentials()).isTrue();
    assertThat(response.getHeaders().getAccessControlExposeHeaders()).contains("X-Request-ID");
  }

  @Test
  @DisplayName(
      "OPTIONS /api/v1/students - Should reject or omit CORS headers for disallowed origin")
  void shouldRejectCorsPreflightFromDisallowedOrigin() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.ORIGIN, "http://evil-attacker.com");
    headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<Void> response =
        testRestTemplate.exchange("/api/v1/students", HttpMethod.OPTIONS, entity, Void.class);

    assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
  }
}
