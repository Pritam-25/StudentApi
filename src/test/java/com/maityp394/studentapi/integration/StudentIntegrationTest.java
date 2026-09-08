package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@DisplayName("Student Resource Integration Tests (/api/v1/students)")
class StudentIntegrationTest extends BaseIntegrationTest {

  @Test
  @DisplayName("GET /students/{id} - Should return student when valid ID is provided")
  void shouldReturnStudentWhenValidIdIsProvided() {
    Student student =
        createTestStudent("John Doe", "john.doe@example.com", "Secret123!", Responsibility.STUDENT);
    String token = createAccessToken(student);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + student.getId(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message"))
        .isEqualTo("Student fetched successfully");
    assertThat((String) documentContext.read("$.data.name")).isEqualTo("John Doe");
    assertThat((String) documentContext.read("$.data.email")).isEqualTo("john.doe@example.com");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  @DisplayName("GET /students/{id} - Should return 400 Bad Request when malformed UUID is supplied")
  void shouldReturnBadRequestWhenMalformedUuidIsProvided() {
    Student caller = createDefaultStudent();
    String token = createAccessToken(caller);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/999", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(400);
    assertThat((String) documentContext.read("$.title")).isEqualTo("Invalid Parameter Type");
    assertThat((String) documentContext.read("$.code")).isEqualTo("INVALID_PARAMETER_TYPE");
    assertThat((String) documentContext.read("$.instance")).isEqualTo("/api/v1/students/999");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  @DisplayName("GET /students/{id} - Should return 404 Not Found when ID does not exist")
  void shouldReturnNotFoundProblemDetailWhenInvalidIdIsProvided() {
    Student caller = createDefaultStudent();
    String token = createAccessToken(caller);
    HttpHeaders headers = createBearerHeaders(token);
    String nonExistentId = "00000000-0000-0000-0000-000000000000";

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + nonExistentId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

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
  @DisplayName("PUT /students/{id} - Should return 409 Conflict when updating to an existing email")
  void shouldReturnConflictWhenUpdatingToExistingEmail() {
    createTestStudent("User One", "user1@example.com", "Secret123!", Responsibility.STUDENT);
    Student s2 =
        createTestStudent("User Two", "user2@example.com", "Secret123!", Responsibility.STUDENT);
    String token = createAccessToken(s2);

    UpdateStudentRequest updateRequest =
        new UpdateStudentRequest("User Two Updated", "user1@example.com");
    HttpHeaders headers = createBearerHeaders(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(updateRequest, headers);

    ResponseEntity<String> updateResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + s2.getId(), HttpMethod.PUT, entity, String.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    DocumentContext documentContext = JsonPath.parse(updateResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(409);
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_EMAIL_ALREADY_EXISTS");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Email Already Exists");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  @DisplayName(
      "PATCH /students/{id} - Should return 409 Conflict when patching to an existing email")
  void shouldReturnConflictWhenPatchingToExistingEmail() {
    createTestStudent("Patch One", "patch1@example.com", "Secret123!", Responsibility.STUDENT);
    Student s2 =
        createTestStudent("Patch Two", "patch2@example.com", "Secret123!", Responsibility.STUDENT);
    String token = createAccessToken(s2);

    PatchStudentRequest patchRequest = new PatchStudentRequest(null, "patch1@example.com");
    HttpHeaders headers = createBearerHeaders(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<PatchStudentRequest> entity = new HttpEntity<>(patchRequest, headers);

    ResponseEntity<String> patchResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + s2.getId(), HttpMethod.PATCH, entity, String.class);

    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    DocumentContext documentContext = JsonPath.parse(patchResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(409);
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_EMAIL_ALREADY_EXISTS");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Email Already Exists");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  @DisplayName(
      "DELETE /students/{id} - Should delete student successfully and return 204 No Content")
  void shouldDeleteStudentSuccessfully() {
    Student student =
        createTestStudent(
            "To Delete", "todelete@example.com", "Secret123!", Responsibility.STUDENT);
    String token = createAccessToken(student);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> deleteResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + student.getId(),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> getResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + student.getId(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName(
      "DELETE /students/{id} - Should return 400 Bad Request when deleting with malformed UUID")
  void shouldReturnBadRequestWhenDeletingWithMalformedUuid() {
    Student caller = createDefaultStudent();
    String token = createAccessToken(caller);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> deleteResponse =
        testRestTemplate.exchange(
            "/api/v1/students/invalid-uuid",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    DocumentContext documentContext = JsonPath.parse(deleteResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(400);
    assertThat((String) documentContext.read("$.code")).isEqualTo("INVALID_PARAMETER_TYPE");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Invalid Parameter Type");
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  @DisplayName(
      "DELETE /students/{id} - Should return 404 Not Found when deleting non-existent student")
  void shouldReturnNotFoundWhenDeletingNonExistentStudent() {
    Student caller = createDefaultStudent();
    String token = createAccessToken(caller);
    HttpHeaders headers = createBearerHeaders(token);
    String nonExistentId = "00000000-0000-0000-0000-000000000000";

    ResponseEntity<String> deleteResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + nonExistentId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    DocumentContext documentContext = JsonPath.parse(deleteResponse.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(404);
    assertThat((String) documentContext.read("$.code")).isEqualTo("STUDENT_NOT_FOUND");
    assertThat((String) documentContext.read("$.title")).isEqualTo("Student Not Found");
    assertThat((String) documentContext.read("$.detail")).contains(nonExistentId);
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }
}
