package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import java.util.List;
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
    assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
        .isNotNull()
        .anyMatch(cookie -> cookie.contains("access_token=") && cookie.contains("HttpOnly"));

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("Registration successful");
    assertThat((String) documentContext.read("$.data.name")).isEqualTo("New Student");
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("new.student@example.com");
    assertThat((String) documentContext.read("$.data.responsibility")).isEqualTo("STUDENT");
    String id = documentContext.read("$.data.id");
    assertThat(id).isNotBlank();
    assertThat((String) documentContext.read("$.data.createdAt")).isNotNull();
    assertThat((String) documentContext.read("$.data.updatedAt")).isNotNull();

    Student saved = studentRepository.findByEmail("new.student@example.com").orElseThrow();
    assertThat(saved.getResponsibility()).isEqualTo(Responsibility.STUDENT);
    assertThat(saved.getPasswordHash())
        .startsWith("$2a$")
        .satisfies(hash -> assertThat(passwordEncoder.matches("Secret123!", hash)).isTrue());
  }

  @Test
  @DisplayName("POST /register - Should register new student with default STUDENT responsibility")
  void shouldRegisterStudentWithDefaultResponsibility() {
    RegisterRequest registerRequest =
        new RegisterRequest("Default Student", "default.student@example.com", "Secret123!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.data.responsibility")).isEqualTo("STUDENT");

    Student saved = studentRepository.findByEmail("default.student@example.com").orElseThrow();
    assertThat(saved.getResponsibility()).isEqualTo(Responsibility.STUDENT);
  }

  @Test
  @DisplayName(
      "POST /register - Should auto-login newly registered user and allow immediate authenticated access")
  void shouldAutoLoginOnRegistrationAndAllowImmediateAccess() {
    RegisterRequest registerRequest =
        new RegisterRequest("Auto Login User", "autologin@example.com", "Secret123!");

    ResponseEntity<String> regResponse =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    List<String> cookies = regResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(cookies).isNotNull();
    String accessTokenCookie =
        cookies.stream().filter(c -> c.startsWith("access_token=")).findFirst().orElseThrow();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, accessTokenCookie);
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

    ResponseEntity<String> meResponse =
        testRestTemplate.exchange("/api/v1/auth/me", HttpMethod.GET, requestEntity, String.class);

    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext doc = JsonPath.parse(meResponse.getBody());
    assertThat((String) doc.read("$.data.email")).isEqualTo("autologin@example.com");
    assertThat((String) doc.read("$.data.name")).isEqualTo("Auto Login User");
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
  @DisplayName(
      "POST /register - Should return 400 Bad Request when password lacks uppercase letter")
  void shouldRejectRegistrationWhenPasswordLacksUppercaseLetter() {
    RegisterRequest request =
        new RegisterRequest("No Uppercase", "no.upper@example.com", "secret123!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext json = JsonPath.parse(response.getBody());
    assertThat((String) json.read("$.code")).isEqualTo("VALIDATION_FAILED");
    assertThat((String) json.read("$.errors.password"))
        .contains("Password must contain at least one uppercase letter");
  }

  @Test
  @DisplayName(
      "POST /register - Should return 400 Bad Request when password lacks special character")
  void shouldRejectRegistrationWhenPasswordLacksSpecialCharacter() {
    RegisterRequest request =
        new RegisterRequest("No Special", "no.special@example.com", "Secret1234");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext json = JsonPath.parse(response.getBody());
    assertThat((String) json.read("$.code")).isEqualTo("VALIDATION_FAILED");
    assertThat((String) json.read("$.errors.password"))
        .contains("Password must contain at least one special character");
  }

  @Test
  @DisplayName(
      "POST /register - Should return 400 Bad Request when password uses whitespace as special character")
  void shouldRejectRegistrationWhenPasswordUsesWhitespaceAsSpecialCharacter() {
    RegisterRequest request =
        new RegisterRequest("Whitespace Special", "whitespace@example.com", "Abcdef1 ");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext json = JsonPath.parse(response.getBody());
    assertThat((String) json.read("$.code")).isEqualTo("VALIDATION_FAILED");
    assertThat((String) json.read("$.errors.password"))
        .contains("Password must contain at least one special character");
  }

  @Test
  @DisplayName("POST /register - Should return 400 Bad Request when password lacks number")
  void shouldRejectRegistrationWhenPasswordLacksNumber() {
    RegisterRequest request =
        new RegisterRequest("No Number", "no.number@example.com", "SecretSpecial!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext json = JsonPath.parse(response.getBody());
    assertThat((String) json.read("$.code")).isEqualTo("VALIDATION_FAILED");
    assertThat((String) json.read("$.errors.password"))
        .contains("Password must contain at least one number");
  }

  @Test
  @DisplayName("POST /register - Should return 400 Bad Request when password is under 8 characters")
  void shouldRejectRegistrationWhenPasswordIsTooShort() {
    RegisterRequest request = new RegisterRequest("Too Short", "too.short@example.com", "Sec1!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext json = JsonPath.parse(response.getBody());
    assertThat((String) json.read("$.code")).isEqualTo("VALIDATION_FAILED");
    assertThat((String) json.read("$.errors.password"))
        .contains("Password must be between 8 and 30 characters");
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
    assertThat(saved.getPasswordHash())
        .isNotEqualTo(rawPassword)
        .startsWith("$2a$")
        .satisfies(hash -> assertThat(passwordEncoder.matches(rawPassword, hash)).isTrue());
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

  @Test
  @DisplayName("POST /register - Should trim whitespace from name and normalize email to lowercase")
  void shouldNormalizeInputDuringRegistrationAndLogin() {
    RegisterRequest registerRequest =
        new RegisterRequest("  Trimmed Name  ", "  Trimmed.Email@EXAMPLE.Com  ", "Secret123!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.data.name")).isEqualTo("Trimmed Name");
    assertThat((String) documentContext.read("$.data.email"))
        .isEqualTo("trimmed.email@example.com");

    // Login with mixed case & untrimmed email should succeed
    LoginRequest loginRequest = new LoginRequest("   TRIMMED.EMAIL@example.COM  ", "Secret123!");
    ResponseEntity<String> loginResponse =
        testRestTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("POST /register - Should reject whitespace-only name with 400 Bad Request")
  void shouldRejectWhitespaceOnlyName() {
    RegisterRequest request = new RegisterRequest("     ", "whitespace@example.com", "Secret123!");

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("VALIDATION_FAILED");
    assertThat((Object) documentContext.read("$.errors.name")).isNotNull();
  }

  @Test
  @DisplayName("POST /refresh - Should rotate refresh token and issue new access token")
  void shouldRefreshTokenSuccessfullyAndRotateTokens() {
    RegisterRequest registerRequest =
        new RegisterRequest("Refresh User", "refresh.user@example.com", "Secret123!");
    ResponseEntity<String> regResponse =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    List<String> cookies = regResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(cookies).isNotNull();

    String refreshTokenCookie =
        cookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();
    String refreshCookieValue = refreshTokenCookie.split(";")[0];

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, refreshCookieValue);
    HttpEntity<Void> refreshEntity = new HttpEntity<>(headers);

    ResponseEntity<String> refreshResponse =
        testRestTemplate.exchange(
            "/api/v1/auth/refresh", HttpMethod.POST, refreshEntity, String.class);

    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<String> newCookies = refreshResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(newCookies).isNotNull();

    String newAccessCookie =
        newCookies.stream().filter(c -> c.startsWith("access_token=")).findFirst().orElseThrow();
    String newRefreshCookie =
        newCookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();

    assertThat(newRefreshCookie).isNotEqualTo(refreshTokenCookie);

    // Validate new access token works against /me
    HttpHeaders meHeaders = new HttpHeaders();
    meHeaders.add(HttpHeaders.COOKIE, newAccessCookie.split(";")[0]);
    ResponseEntity<String> meResponse =
        testRestTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(meHeaders), String.class);

    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext doc = JsonPath.parse(meResponse.getBody());
    assertThat((String) doc.read("$.data.email")).isEqualTo("refresh.user@example.com");
  }

  @Test
  @DisplayName(
      "POST /refresh - Replaying an old refresh token should revoke session and return 401")
  void shouldDetectRefreshTokenReplayAndRevokeSession() {
    RegisterRequest registerRequest =
        new RegisterRequest("Replay User", "replay.user@example.com", "Secret123!");
    ResponseEntity<String> regResponse =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    List<String> cookies = regResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    String initialRefreshToken =
        cookies.stream()
            .filter(c -> c.startsWith("refresh_token="))
            .findFirst()
            .orElseThrow()
            .split(";")[0];

    // First rotation (valid)
    HttpHeaders headers1 = new HttpHeaders();
    headers1.add(HttpHeaders.COOKIE, initialRefreshToken);
    ResponseEntity<String> rotate1 =
        testRestTemplate.exchange(
            "/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers1), String.class);
    assertThat(rotate1.getStatusCode()).isEqualTo(HttpStatus.OK);

    List<String> rotatedCookies = rotate1.getHeaders().get(HttpHeaders.SET_COOKIE);
    String secondRefreshToken =
        rotatedCookies.stream()
            .filter(c -> c.startsWith("refresh_token="))
            .findFirst()
            .orElseThrow()
            .split(";")[0];

    // Attempt replay with initialRefreshToken (already rotated)
    HttpHeaders replayHeaders = new HttpHeaders();
    replayHeaders.add(HttpHeaders.COOKIE, initialRefreshToken);
    ResponseEntity<String> replayResponse =
        testRestTemplate.exchange(
            "/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(replayHeaders), String.class);

    // Replay must be rejected with 401 Unauthorized
    assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // Session is now revoked, so secondRefreshToken should also fail
    HttpHeaders headers2 = new HttpHeaders();
    headers2.add(HttpHeaders.COOKIE, secondRefreshToken);
    ResponseEntity<String> revokedResponse =
        testRestTemplate.exchange(
            "/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers2), String.class);
    assertThat(revokedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /logout-all - Should invalidate all active sessions for the student")
  void shouldLogoutAllSessions() {
    createTestStudent(
        "Logout All User", "logoutall@example.com", "Secret123!", Responsibility.STUDENT);

    // Session 1
    LoginRequest loginRequest1 = new LoginRequest("logoutall@example.com", "Secret123!");
    ResponseEntity<String> login1 =
        testRestTemplate.postForEntity("/api/v1/auth/login", loginRequest1, String.class);
    assertThat(login1.getStatusCode()).isEqualTo(HttpStatus.OK);
    String refresh1 =
        login1.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
            .filter(c -> c.startsWith("refresh_token="))
            .findFirst()
            .orElseThrow()
            .split(";")[0];
    String access1 =
        login1.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
            .filter(c -> c.startsWith("access_token="))
            .findFirst()
            .orElseThrow()
            .split(";")[0];

    // Session 2
    ResponseEntity<String> login2 =
        testRestTemplate.postForEntity("/api/v1/auth/login", loginRequest1, String.class);
    assertThat(login2.getStatusCode()).isEqualTo(HttpStatus.OK);
    String refresh2 =
        login2.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
            .filter(c -> c.startsWith("refresh_token="))
            .findFirst()
            .orElseThrow()
            .split(";")[0];

    // Call /logout-all with access1 as Bearer token
    String rawAccessToken = access1.replace("access_token=", "");
    HttpHeaders logoutHeaders = createBearerHeaders(rawAccessToken);
    ResponseEntity<String> logoutResponse =
        testRestTemplate.exchange(
            "/api/v1/auth/logout-all",
            HttpMethod.POST,
            new HttpEntity<>(logoutHeaders),
            String.class);
    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Both refresh tokens should now fail
    HttpHeaders refreshHeaders1 = new HttpHeaders();
    refreshHeaders1.add(HttpHeaders.COOKIE, refresh1);
    assertThat(
            testRestTemplate
                .exchange(
                    "/api/v1/auth/refresh",
                    HttpMethod.POST,
                    new HttpEntity<>(refreshHeaders1),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);

    HttpHeaders refreshHeaders2 = new HttpHeaders();
    refreshHeaders2.add(HttpHeaders.COOKIE, refresh2);
    assertThat(
            testRestTemplate
                .exchange(
                    "/api/v1/auth/refresh",
                    HttpMethod.POST,
                    new HttpEntity<>(refreshHeaders2),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
