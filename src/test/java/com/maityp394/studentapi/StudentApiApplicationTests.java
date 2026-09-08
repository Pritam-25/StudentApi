package com.maityp394.studentapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.JwtService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class StudentApiApplicationTests {

  @Autowired TestRestTemplate testRestTemplate;
  @Autowired StudentRepository studentRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JwtService jwtService;

  private String authToken;
  private UUID authenticatedStudentId;

  @BeforeEach
  void setUp() {
    studentRepository.deleteAll();
    Student student = new Student();
    student.setName("Auth User");
    student.setEmail("auth.user@example.com");
    student.setPasswordHash(passwordEncoder.encode("Password123!"));
    student.setResponsibility(Responsibility.STUDENT);
    Student saved = studentRepository.save(student);
    authenticatedStudentId = saved.getId();
    authToken = jwtService.generateAccessToken(saved);

    testRestTemplate
        .getRestTemplate()
        .setInterceptors(
            List.of(
                (request, body, execution) -> {
                  if (request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null
                      && request.getHeaders().getFirst("X-Skip-Auth") == null) {
                    request.getHeaders().setBearerAuth(authToken);
                  }
                  return execution.execute(request, body);
                }));
  }

  @Test
  void shouldReturnStudentWhenValidIdIsProvided() {
    RegisterRequest newStudent =
        new RegisterRequest("John Doe", "john.doe@example.com", "Secret123!");
    ResponseEntity<String> createResponse =
        testRestTemplate.postForEntity("/api/v1/auth/register", newStudent, String.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    DocumentContext createContext = JsonPath.parse(createResponse.getBody());
    String id = createContext.read("$.data.id");
    assertThat(id).isNotBlank();
    assertThat((String) createContext.read("$.message")).isEqualTo("Registration successful");
    assertThat((String) createContext.read("$.timestamp")).isNotBlank();

    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/api/v1/students/" + id, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message"))
        .isEqualTo("Student fetched successfully");
    assertThat((String) documentContext.read("$.data.name")).isEqualTo("John Doe");
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("john.doe@example.com");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  void shouldReturnBadRequestWhenMalformedUuidIsProvided() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/api/v1/students/999", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(400);
    assertThat((String) documentContext.read("$.title")).isEqualTo("Invalid Parameter Type");
    assertThat((String) documentContext.read("$.code")).isEqualTo("INVALID_PARAMETER_TYPE");
    assertThat((String) documentContext.read("$.instance")).isEqualTo("/api/v1/students/999");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  void shouldReturnNotFoundProblemDetailWhenInvalidIdIsProvided() {
    String nonExistentId = "00000000-0000-0000-0000-000000000000";
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/api/v1/students/" + nonExistentId, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(404);
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Not Found");
    assertThat((String) documentContext.read("$.detail"))
        .isEqualTo("Student not found: " + nonExistentId);
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_NOT_FOUND");
    assertThat((String) documentContext.read("$.instance"))
        .isEqualTo("/api/v1/students/" + nonExistentId);
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
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
  void shouldReturnConflictWhenUpdatingToExistingEmail() {
    RegisterRequest s1 = new RegisterRequest("User One", "user1@example.com", "Secret123!");
    ResponseEntity<String> r1 =
        testRestTemplate.postForEntity("/api/v1/auth/register", s1, String.class);
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    RegisterRequest s2 = new RegisterRequest("User Two", "user2@example.com", "Secret123!");
    ResponseEntity<String> r2 =
        testRestTemplate.postForEntity("/api/v1/auth/register", s2, String.class);
    assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String s2Id = JsonPath.parse(r2.getBody()).read("$.data.id");

    UpdateStudentRequest updateRequest =
        new UpdateStudentRequest("User Two Updated", "user1@example.com");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(updateRequest, headers);

    ResponseEntity<String> updateResponse =
        testRestTemplate.exchange("/api/v1/students/" + s2Id, HttpMethod.PUT, entity, String.class);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    DocumentContext documentContext = JsonPath.parse(updateResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(409);
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_EMAIL_ALREADY_EXISTS");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Email Already Exists");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  void shouldReturnConflictWhenPatchingToExistingEmail() {
    RegisterRequest s1 = new RegisterRequest("Patch One", "patch1@example.com", "Secret123!");
    ResponseEntity<String> r1 =
        testRestTemplate.postForEntity("/api/v1/auth/register", s1, String.class);
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    RegisterRequest s2 = new RegisterRequest("Patch Two", "patch2@example.com", "Secret123!");
    ResponseEntity<String> r2 =
        testRestTemplate.postForEntity("/api/v1/auth/register", s2, String.class);
    assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String s2Id = JsonPath.parse(r2.getBody()).read("$.data.id");

    PatchStudentRequest patchRequest = new PatchStudentRequest(null, "patch1@example.com");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<PatchStudentRequest> entity = new HttpEntity<>(patchRequest, headers);

    ResponseEntity<String> patchResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + s2Id, HttpMethod.PATCH, entity, String.class);
    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    DocumentContext documentContext = JsonPath.parse(patchResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(409);
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_EMAIL_ALREADY_EXISTS");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Email Already Exists");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  void shouldReturnBadRequestWhenDeletingWithMalformedUuid() {
    ResponseEntity<String> deleteResponse =
        testRestTemplate.exchange(
            "/api/v1/students/invalid-uuid", HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    DocumentContext documentContext = JsonPath.parse(deleteResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(400);
    assertThat((String) documentContext.read("$.code")).isEqualTo("INVALID_PARAMETER_TYPE");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Invalid Parameter Type");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  void shouldReturnNotFoundWhenDeletingNonExistentStudent() {
    String nonExistentId = "00000000-0000-0000-0000-000000000000";
    ResponseEntity<String> deleteResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + nonExistentId, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    DocumentContext documentContext = JsonPath.parse(deleteResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(404);
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_NOT_FOUND");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Not Found");
    assertThat((String) documentContext.read("$.detail")).contains(nonExistentId);
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  void shouldDeleteStudentSuccessfully() {
    RegisterRequest student =
        new RegisterRequest("To Delete", "todelete@example.com", "Secret123!");
    ResponseEntity<String> createResponse =
        testRestTemplate.postForEntity("/api/v1/auth/register", student, String.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String id = JsonPath.parse(createResponse.getBody()).read("$.data.id");

    ResponseEntity<String> deleteResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + id, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> getResponse =
        testRestTemplate.getForEntity("/api/v1/students/" + id, String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
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
  void shouldGenerateRequestIdHeaderWhenMissingInRequest() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/api/v1/students", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    String requestId = response.getHeaders().getFirst("X-Request-ID");
    assertThat(requestId).isNotBlank();
    assertThat(UUID.fromString(requestId)).isNotNull();
  }

  @Test
  void shouldPreserveIncomingRequestIdHeader() {
    String customRequestId = "custom-trace-id-12345";
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Request-ID", customRequestId);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/students", HttpMethod.GET, entity, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst("X-Request-ID")).isEqualTo(customRequestId);
  }

  @Test
  void shouldIncludeRequestIdHeaderOnExceptionResponses() {
    String customRequestId = "error-trace-id-999";
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Request-ID", customRequestId);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/invalid-uuid", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getHeaders().getFirst("X-Request-ID")).isEqualTo(customRequestId);
  }

  @Test
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
  void shouldFailRegistrationWhenEmailAlreadyExists() {
    RegisterRequest registerRequest =
        new RegisterRequest("Duplicate User", "auth.user@example.com", "Secret123!");
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_EMAIL_ALREADY_EXISTS");
  }

  @Test
  void shouldLogoutSuccessfully() {
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/logout", null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("Logged out successfully");

    String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).isNotNull();
    assertThat(setCookie).contains("access_token=");
    assertThat(setCookie).contains("Max-Age=0");
    assertThat(setCookie).contains("HttpOnly");
  }

  @Test
  void shouldLoginSuccessfullyAndSetHttpOnlyCookie() {
    LoginRequest loginRequest = new LoginRequest("auth.user@example.com", "Password123!");
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Verify Set-Cookie header contains HttpOnly, Path=/, SameSite=Lax, and access_token
    String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).isNotNull();
    assertThat(setCookie).contains("access_token=");
    assertThat(setCookie).contains("HttpOnly");
    assertThat(setCookie).contains("Path=/");
    assertThat(setCookie).contains("SameSite=Lax");

    // Verify JSON body contains user details but NO accessToken
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("Login successful");
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("auth.user@example.com");
    assertThat((String) documentContext.read("$.data.id"))
        .isEqualTo(authenticatedStudentId.toString());
    assertThat(response.getBody()).doesNotContain("accessToken");
  }

  @Test
  void shouldFailLoginWithBadCredentials() {
    LoginRequest loginRequest = new LoginRequest("auth.user@example.com", "WrongPassword!");
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  void shouldFetchCurrentUserWithJwt() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/api/v1/auth/me", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message")).isEqualTo("User fetched successfully");
    assertThat((String) documentContext.read("$.data.id"))
        .isEqualTo(authenticatedStudentId.toString());
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("auth.user@example.com");
  }

  @Test
  void shouldRejectUnauthenticatedAccessToProtectedEndpoint() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Skip-Auth", "true");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/students", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("UNAUTHORIZED");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Unauthorized");
    assertThat((String) documentContext.read("$.detail"))
        .isEqualTo("Authentication is required to access this resource");
  }

  @Test
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
  void shouldAuthenticateUsingHttpOnlyCookieWithoutAuthorizationHeader() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Skip-Auth", "true");
    headers.add(HttpHeaders.COOKIE, "access_token=" + authToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/students", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldPrioritizeAuthorizationHeaderOverCookie() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Skip-Auth", "true");
    headers.setBearerAuth(authToken);
    headers.add(HttpHeaders.COOKIE, "access_token=invalid.cookie.token");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange("/api/v1/students", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldRejectStateChangingCookieRequestWithoutCsrfToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Skip-Auth", "true");
    headers.add(HttpHeaders.COOKIE, "access_token=" + authToken);
    UpdateStudentRequest update = new UpdateStudentRequest("Name", "newemail@example.com");
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(update, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + authenticatedStudentId, HttpMethod.PUT, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("CSRF_INVALID");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Forbidden");
    assertThat((String) documentContext.read("$.detail"))
        .isEqualTo("Invalid or missing CSRF token");
  }

  @Test
  void shouldAcceptStateChangingBearerRequestWithoutCsrfToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);
    UpdateStudentRequest update =
        new UpdateStudentRequest("Bearer User", "bearer.update@example.com");
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(update, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + authenticatedStudentId, HttpMethod.PUT, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldAcceptStateChangingCookieRequestWithCsrfToken() {
    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set("X-Skip-Auth", "true");
    getHeaders.add(HttpHeaders.COOKIE, "access_token=" + authToken);
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

    HttpHeaders putHeaders = new HttpHeaders();
    putHeaders.set("X-Skip-Auth", "true");
    putHeaders.add(HttpHeaders.COOKIE, "access_token=" + authToken + "; XSRF-TOKEN=" + xsrfToken);
    putHeaders.set("X-XSRF-TOKEN", xsrfToken);
    UpdateStudentRequest update =
        new UpdateStudentRequest("Updated Auth User", "auth.user@example.com");
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(update, putHeaders);

    ResponseEntity<String> putResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + authenticatedStudentId, HttpMethod.PUT, entity, String.class);

    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
