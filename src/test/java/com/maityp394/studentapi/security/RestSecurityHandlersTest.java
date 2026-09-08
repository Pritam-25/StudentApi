package com.maityp394.studentapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;

@SpringBootTest
class RestSecurityHandlersTest {

  @Autowired private RestAuthenticationEntryPoint authenticationEntryPoint;
  @Autowired private RestAccessDeniedHandler accessDeniedHandler;

  @Test
  void authenticationEntryPointShouldReturnUnauthorizedWhenNoCredentialsProvided()
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students");
    MockHttpServletResponse response = new MockHttpServletResponse();

    InsufficientAuthenticationException ex =
        new InsufficientAuthenticationException("Full authentication is required");

    this.authenticationEntryPoint.commence(request, response, ex);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");

    DocumentContext json = JsonPath.parse(response.getContentAsString());
    assertThat((String) json.read("$.code")).isEqualTo("UNAUTHORIZED");
    assertThat((String) json.read("$.title")).isEqualTo("Unauthorized");
    assertThat((String) json.read("$.detail"))
        .isEqualTo("Authentication is required to access this resource");
    assertThat((String) json.read("$.instance")).isEqualTo("/api/v1/students");
  }

  @Test
  void authenticationEntryPointShouldReturnInvalidTokenWhenBearerTokenFails() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    OAuth2AuthenticationException ex =
        new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Jwt expired", null));

    this.authenticationEntryPoint.commence(request, response, ex);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
        .contains("error=\"invalid_token\"");

    DocumentContext json = JsonPath.parse(response.getContentAsString());
    assertThat((String) json.read("$.code")).isEqualTo("INVALID_TOKEN");
    assertThat((String) json.read("$.title")).isEqualTo("Unauthorized");
    assertThat((String) json.read("$.detail")).isEqualTo("The access token is invalid or expired");
  }

  @Test
  void accessDeniedHandlerShouldReturnCsrfInvalidWhenCsrfValidationFails() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/students/123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    InvalidCsrfTokenException ex =
        new InvalidCsrfTokenException(
            new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "expected"), "actual");

    this.accessDeniedHandler.handle(request, response, ex);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    DocumentContext json = JsonPath.parse(response.getContentAsString());
    assertThat((String) json.read("$.code")).isEqualTo("CSRF_INVALID");
    assertThat((String) json.read("$.title")).isEqualTo("Forbidden");
    assertThat((String) json.read("$.detail")).isEqualTo("Invalid or missing CSRF token");
    assertThat((String) json.read("$.instance")).isEqualTo("/api/v1/students/123");
  }

  @Test
  void accessDeniedHandlerShouldReturnForbiddenWhenAuthorizationFails() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin");
    MockHttpServletResponse response = new MockHttpServletResponse();

    AccessDeniedException ex = new AccessDeniedException("Access is denied");

    this.accessDeniedHandler.handle(request, response, ex);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    DocumentContext json = JsonPath.parse(response.getContentAsString());
    assertThat((String) json.read("$.code")).isEqualTo("FORBIDDEN");
    assertThat((String) json.read("$.title")).isEqualTo("Forbidden");
    assertThat((String) json.read("$.detail"))
        .isEqualTo("You do not have permission to access this resource");
    assertThat((String) json.read("$.instance")).isEqualTo("/api/v1/admin");
  }
}
