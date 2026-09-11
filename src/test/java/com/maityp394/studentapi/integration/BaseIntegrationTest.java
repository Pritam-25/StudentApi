package com.maityp394.studentapi.integration;

import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Base class for all end-to-end integration tests.
 *
 * <p>Configures an embedded servlet container on a random port and wires a {@link
 * TestRestTemplate}. All subclasses share identical merged context configuration to maximize Spring
 * TestContext caching.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class BaseIntegrationTest {

  @Autowired protected TestRestTemplate testRestTemplate;
  @Autowired protected StudentRepository studentRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected JwtService jwtService;

  @BeforeEach
  void cleanDatabase() {
    studentRepository.deleteAll();
  }

  /**
   * Helper to persist a new student with encoded password.
   *
   * @param name student's display name
   * @param email unique email address
   * @param rawPassword plain text password to encode
   * @param responsibility student responsibility role
   * @return persisted {@link Student} entity
   */
  protected Student createTestStudent(
      String name, String email, String rawPassword, Responsibility responsibility) {
    Student student = new Student(name, email, passwordEncoder.encode(rawPassword), responsibility);
    return studentRepository.save(student);
  }

  /**
   * Helper to persist a standard test student ("auth.user@example.com").
   *
   * @return persisted {@link Student} entity
   */
  protected Student createDefaultStudent() {
    return createTestStudent(
        "Auth User", "auth.user@example.com", "Password123!", Responsibility.STUDENT);
  }

  /**
   * Helper to persist a test class representative ("rep.user@example.com").
   *
   * @return persisted {@link Student} entity with CLASS_REPRESENTATIVE responsibility
   */
  protected Student createClassRepresentative() {
    return createTestStudent(
        "Class Rep", "rep.user@example.com", "Password123!", Responsibility.CLASS_REPRESENTATIVE);
  }

  /**
   * Helper to generate a signed JWT access token for the given student.
   *
   * @param student student entity
   * @return signed JWT token string
   */
  protected String createAccessToken(Student student) {
    return jwtService.generateAccessToken(student);
  }

  /**
   * Helper to create HTTP headers with a Bearer Authorization token.
   *
   * @param token JWT token
   * @return {@link HttpHeaders} with Authorization header
   */
  protected HttpHeaders createBearerHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  /**
   * Helper to construct Cookie and optional CSRF headers for cookie-based authentication tests.
   *
   * @param accessToken JWT token to set in cookie
   * @param csrfToken CSRF token value, or null if none
   * @return {@link HttpHeaders} with Cookie and X-XSRF-TOKEN
   */
  protected HttpHeaders createCookieHeaders(String accessToken, String csrfToken) {
    HttpHeaders headers = new HttpHeaders();
    StringBuilder cookie = new StringBuilder();
    if (accessToken != null) {
      cookie.append("access_token=").append(accessToken);
    }
    if (csrfToken != null) {
      if (!cookie.isEmpty()) {
        cookie.append("; ");
      }
      cookie.append("XSRF-TOKEN=").append(csrfToken);
      headers.set("X-XSRF-TOKEN", csrfToken);
    }
    headers.set(HttpHeaders.COOKIE, cookie.toString());
    return headers;
  }
}
