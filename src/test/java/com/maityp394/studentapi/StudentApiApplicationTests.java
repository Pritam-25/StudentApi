package com.maityp394.studentapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.repository.StudentRepository;
import java.net.URI;
import java.util.UUID;
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

  @Test
  void shouldReturnStudentWhenValidIdIsProvided() {
    CreateStudentRequest newStudent =
        new CreateStudentRequest("John Doe", "john.doe@example.com", "Secret123!");
    ResponseEntity<String> createResponse =
        testRestTemplate.postForEntity("/api/v1/students", newStudent, String.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    DocumentContext createContext = JsonPath.parse(createResponse.getBody());
    String id = createContext.read("$.data.id");
    assertThat(id).isNotBlank();
    assertThat((String) createContext.read("$.message")).isEqualTo("Student created successfully");
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
  void shouldCreateNewStudent() {
    CreateStudentRequest newStudent =
        new CreateStudentRequest("Jane Doe", "jane.doe@example.com", "Secret123!");
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/students", newStudent, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    URI locationOfNewStudent = response.getHeaders().getLocation();
    assertThat(locationOfNewStudent).isNotNull();
    ResponseEntity<String> getResponse =
        testRestTemplate.getForEntity(locationOfNewStudent, String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturnValidationProblemDetailWhenInvalidPayload() {
    CreateStudentRequest invalidStudent = new CreateStudentRequest("", "not-an-email", "short");
    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/students", invalidStudent, String.class);

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
  void shouldReturnConflictWhenCreatingDuplicateEmail() {
    CreateStudentRequest first =
        new CreateStudentRequest("Alice One", "alice.dup@example.com", "Secret123!");
    ResponseEntity<String> firstResponse =
        testRestTemplate.postForEntity("/api/v1/students", first, String.class);
    assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    CreateStudentRequest duplicate =
        new CreateStudentRequest("Alice Two", "alice.dup@example.com", "Secret123!");
    ResponseEntity<String> dupResponse =
        testRestTemplate.postForEntity("/api/v1/students", duplicate, String.class);
    assertThat(dupResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    DocumentContext documentContext = JsonPath.parse(dupResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(409);
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Email Already Exists");
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_EMAIL_ALREADY_EXISTS");
    assertThat((String) documentContext.read("$.detail")).contains("alice.dup@example.com");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  void shouldReturnConflictWhenUpdatingToExistingEmail() {
    CreateStudentRequest s1 =
        new CreateStudentRequest("User One", "user1@example.com", "Secret123!");
    ResponseEntity<String> r1 =
        testRestTemplate.postForEntity("/api/v1/students", s1, String.class);
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    CreateStudentRequest s2 =
        new CreateStudentRequest("User Two", "user2@example.com", "Secret123!");
    ResponseEntity<String> r2 =
        testRestTemplate.postForEntity("/api/v1/students", s2, String.class);
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
    CreateStudentRequest s1 =
        new CreateStudentRequest("Patch One", "patch1@example.com", "Secret123!");
    ResponseEntity<String> r1 =
        testRestTemplate.postForEntity("/api/v1/students", s1, String.class);
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    CreateStudentRequest s2 =
        new CreateStudentRequest("Patch Two", "patch2@example.com", "Secret123!");
    ResponseEntity<String> r2 =
        testRestTemplate.postForEntity("/api/v1/students", s2, String.class);
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
    CreateStudentRequest student =
        new CreateStudentRequest("To Delete", "todelete@example.com", "Secret123!");
    ResponseEntity<String> createResponse =
        testRestTemplate.postForEntity("/api/v1/students", student, String.class);
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
    CreateStudentRequest request =
        new CreateStudentRequest("Security Test", "security.test@example.com", rawPassword);

    ResponseEntity<String> response =
        testRestTemplate.postForEntity("/api/v1/students", request, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    String idStr = JsonPath.parse(response.getBody()).read("$.data.id");
    UUID id = UUID.fromString(idStr);

    Student saved = studentRepository.findById(id).orElseThrow();
    assertThat(saved.getPassword()).isNotEqualTo(rawPassword).startsWith("$2a$");
    assertThat(passwordEncoder.matches(rawPassword, saved.getPassword())).isTrue();
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
}
