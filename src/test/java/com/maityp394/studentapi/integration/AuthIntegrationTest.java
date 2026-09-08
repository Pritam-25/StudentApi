package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.entity.Student;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Authentication Integration Tests (/api/v1/auth)")
class AuthIntegrationTest extends BaseIntegrationTest {

  @Test
  @DisplayName("POST /register - Should register new student successfully and return 201 Created")
  void shouldRegisterNewStudentSuccessfully() {
    RegisterRequest registerRequest =
        new RegisterRequest("New Student", "new.student@example.com", "Secret123!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).isNotNull();

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("Registration successful");
    assertThat((String) documentContext.read("$.data.name")).isEqualTo("New Student");
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("new.student@example.com");
    String id = documentContext.read("$.data.id");
    assertThat(id).isNotBlank();

    Student saved = studentRepository.findByEmail("new.student@example.com").orElseThrow();
    assertThat(saved.getPasswordHash()).startsWith("$2a$");
    assertThat(passwordEncoder.matches("Secret123!", saved.getPasswordHash())).isTrue();
  }

  @Test
  @DisplayName("POST /register - Should fail with 409 Conflict when email is already registered")
  void shouldFailRegistrationWhenEmailAlreadyExists() {
    createDefaultStudent();

    RegisterRequest registerRequest =
        new RegisterRequest("Duplicate User", "auth.user@example.com", "Secret123!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_EMAIL_ALREADY_EXISTS");
  }

  @Test
  @DisplayName(
      "POST /register - Should return 400 Bad Request with validation details on invalid payload")
  void shouldReturnValidationProblemDetailWhenInvalidPayload() {
    RegisterRequest invalidStudent = new RegisterRequest("", "not-an-email", "short");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", invalidStudent, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(400);
    assertThat((String) documentContext.read("$.title")).isEqualTo("Validation Failed");
    assertThat((String) documentContext.read("$.code")).isEqualTo("VALIDATION_FAILED");
    assertThat((String) documentContext.read("$.detail"))
        .isEqualTo("One or more fields are invalid");
    assertThat((Object) documentContext.read("$.errors.name")).isNotNull();
    assertThat((Object) documentContext.read("$.errors.email")).isNotNull();
    assertThat((Object) documentContext.read("$.errors.password")).isNotNull();
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  @DisplayName("POST /register - Should hash student password using BCrypt upon registration")
  void shouldHashPasswordWhenCreatingStudent() {
    String rawPassword = "MySecurePassword123!";
    RegisterRequest request =
        new RegisterRequest("Security Test", "security.test@example.com", rawPassword);

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", request, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    String idStr = JsonPath.parse(response.getBody()).read("$.data.id");
    UUID id = UUID.fromString(idStr);

    Student saved = studentRepository.findById(id).orElseThrow();
    assertThat(saved.getPasswordHash()).isNotEqualTo(rawPassword).startsWith("$2a$");
    assertThat(passwordEncoder.matches(rawPassword, saved.getPasswordHash())).isTrue();
  }

  @Test
  @DisplayName("POST /login - Should login successfully and set HttpOnly access_token cookie")
  void shouldLoginSuccessfullyAndSetHttpOnlyCookie() {
    Student student = createDefaultStudent();

    LoginRequest loginRequest = new LoginRequest("auth.user@example.com", "Password123!");
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .isNotNull()
        .contains("access_token=")
        .contains("HttpOnly")
        .contains("Path=/")
        .contains("SameSite=Lax");

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("Login successful");
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("auth.user@example.com");
    assertThat((String) documentContext.read("$.data.id")).isEqualTo(student.getId().toString());
    assertThat(response.getBody()).doesNotContain("accessToken");
  }

  @Test
  @DisplayName("POST /login - Should return 401 Unauthorized when password is incorrect")
  void shouldFailLoginWithBadCredentials() {
    createDefaultStudent();

    LoginRequest loginRequest = new LoginRequest("auth.user@example.com", "WrongPassword!");
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  @DisplayName("GET /me - Should fetch authenticated student profile with valid JWT Bearer token")
  void shouldFetchCurrentUserWithJwt() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("User fetched successfully");
    assertThat((String) documentContext.read("$.data.id")).isEqualTo(student.getId().toString());
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("auth.user@example.com");
  }

  @Test
  @DisplayName("POST /logout - Should clear access_token cookie by setting Max-Age to 0")
  void shouldLogoutSuccessfully() {
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/logout", null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("Logged out successfully");

    String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .isNotNull()
        .contains("access_token=")
        .contains("Max-Age=0")
        .contains("HttpOnly");
  }
}
