package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateResponsibilityRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
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
      "DELETE /students/{id} - Should return 404 Not Found when deleting non-existent student owned by caller")
  void shouldReturnNotFoundWhenDeletingNonExistentStudent() {
    UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    Student phantom = new Student();
    phantom.setId(nonExistentId);
    phantom.setEmail("phantom@example.com");
    phantom.setResponsibility(Responsibility.STUDENT);
    String token = createAccessToken(phantom);
    HttpHeaders headers = createBearerHeaders(token);

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
    assertThat((String) documentContext.read("$.detail")).contains(nonExistentId.toString());
    assertThat((String) documentContext.read("$.timestamp")).isNotBlank();
  }

  @Test
  @DisplayName(
      "PUT /students/{id} - Should return 403 Forbidden when caller attempts to update another student")
  void shouldReturnForbiddenWhenUpdatingAnotherStudent() {
    Student studentA =
        createTestStudent(
            "Student A", "studentA@example.com", "Secret123!", Responsibility.STUDENT);
    Student studentB =
        createTestStudent(
            "Student B", "studentB@example.com", "Secret123!", Responsibility.STUDENT);
    String tokenA = createAccessToken(studentA);

    UpdateStudentRequest updateRequest =
        new UpdateStudentRequest("Hacked Name", "hacked@example.com");
    HttpHeaders headers = createBearerHeaders(tokenA);
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<UpdateStudentRequest> entity = new HttpEntity<>(updateRequest, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + studentB.getId(), HttpMethod.PUT, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(403);
    assertThat((String) documentContext.read("$.code")).isEqualTo("FORBIDDEN");
  }

  @Test
  @DisplayName(
      "PATCH /students/{id} - Should return 403 Forbidden when caller attempts to patch another student")
  void shouldReturnForbiddenWhenPatchingAnotherStudent() {
    Student studentA =
        createTestStudent(
            "Student A", "studentA.patch@example.com", "Secret123!", Responsibility.STUDENT);
    Student studentB =
        createTestStudent(
            "Student B", "studentB.patch@example.com", "Secret123!", Responsibility.STUDENT);
    String tokenA = createAccessToken(studentA);

    PatchStudentRequest patchRequest = new PatchStudentRequest("Hacked Name", null);
    HttpHeaders headers = createBearerHeaders(tokenA);
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<PatchStudentRequest> entity = new HttpEntity<>(patchRequest, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + studentB.getId(), HttpMethod.PATCH, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(403);
    assertThat((String) documentContext.read("$.code")).isEqualTo("FORBIDDEN");
  }

  @Test
  @DisplayName(
      "DELETE /students/{id} - Should return 403 Forbidden when caller attempts to delete another student")
  void shouldReturnForbiddenWhenDeletingAnotherStudent() {
    Student studentA =
        createTestStudent(
            "Student A", "studentA.del@example.com", "Secret123!", Responsibility.STUDENT);
    Student studentB =
        createTestStudent(
            "Student B", "studentB.del@example.com", "Secret123!", Responsibility.STUDENT);
    String tokenA = createAccessToken(studentA);
    HttpHeaders headers = createBearerHeaders(tokenA);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + studentB.getId(),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(403);
    assertThat((String) documentContext.read("$.code")).isEqualTo("FORBIDDEN");
  }

  @Test
  @DisplayName(
      "GET /students - Should return 403 Forbidden when standard STUDENT attempts to list all students")
  void shouldReturnForbiddenWhenStudentListsAllStudents() {
    Student student = createDefaultStudent();
    String token = createAccessToken(student);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(403);
    assertThat((String) documentContext.read("$.code")).isEqualTo("FORBIDDEN");
  }

  @Test
  @DisplayName(
      "GET /students - Should return 200 OK with student roster when caller is CLASS_REPRESENTATIVE")
  void shouldReturnStudentsListWhenCallerIsClassRepresentative() {
    Student cr = createClassRepresentative();
    createTestStudent("Student One", "s1@example.com", "Secret123!", Responsibility.STUDENT);
    createTestStudent("Student Two", "s2@example.com", "Secret123!", Responsibility.STUDENT);

    String token = createAccessToken(cr);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message"))
        .isEqualTo("Students fetched successfully");
    List<?> students = documentContext.read("$.data");
    assertThat(students).hasSize(3);
    assertThat((String) documentContext.read("$.data[0].createdAt")).isNotBlank();
    assertThat((String) documentContext.read("$.data[0].updatedAt")).isNotBlank();
  }

  @Test
  @DisplayName("GET /students - Should filter students by responsibility")
  void shouldFilterStudentsByResponsibility() {
    Student cr = createClassRepresentative();
    createTestStudent("Student Alpha", "alpha@example.com", "Secret123!", Responsibility.STUDENT);
    createTestStudent("Student Beta", "beta@example.com", "Secret123!", Responsibility.STUDENT);

    String token = createAccessToken(cr);
    HttpHeaders headers = createBearerHeaders(token);

    // Filter by STUDENT
    ResponseEntity<String> studentFilterResponse =
        testRestTemplate.exchange(
            "/api/v1/students?responsibility=STUDENT",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(studentFilterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext studentJson = JsonPath.parse(studentFilterResponse.getBody());
    List<String> studentRoles = studentJson.read("$.data[*].responsibility");
    assertThat(studentRoles).containsOnly("STUDENT").hasSize(2);

    // Filter by CLASS_REPRESENTATIVE
    ResponseEntity<String> crFilterResponse =
        testRestTemplate.exchange(
            "/api/v1/students?responsibility=CLASS_REPRESENTATIVE",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(crFilterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext crJson = JsonPath.parse(crFilterResponse.getBody());
    List<String> crRoles = crJson.read("$.data[*].responsibility");
    assertThat(crRoles).containsOnly("CLASS_REPRESENTATIVE").hasSize(1);
  }

  @Test
  @DisplayName("GET /students - Should sort students by createdAt descending")
  void shouldSortStudentsByCreatedAtDescending() {
    Student cr = createClassRepresentative();
    createTestStudent("First Added", "first@example.com", "Secret123!", Responsibility.STUDENT);
    createTestStudent("Second Added", "second@example.com", "Secret123!", Responsibility.STUDENT);

    String token = createAccessToken(cr);
    HttpHeaders headers = createBearerHeaders(token);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students?sort=createdAt,desc",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext json = JsonPath.parse(response.getBody());
    List<String> createdTimestamps = json.read("$.data[*].createdAt");
    // Verify non-null and non-blank
    assertThat(createdTimestamps).hasSize(3).allSatisfy(ts -> assertThat(ts).isNotBlank());
  }

  @Test
  @DisplayName(
      "PATCH /students/{id}/responsibility - Should allow CLASS_REPRESENTATIVE to update student responsibility")
  void shouldAllowClassRepresentativeToUpdateResponsibility() {
    Student cr = createClassRepresentative();
    Student student = createDefaultStudent();

    String token = createAccessToken(cr);
    HttpHeaders headers = createBearerHeaders(token);
    headers.setContentType(MediaType.APPLICATION_JSON);

    UpdateResponsibilityRequest request =
        new UpdateResponsibilityRequest(Responsibility.CLASS_REPRESENTATIVE);
    HttpEntity<UpdateResponsibilityRequest> entity = new HttpEntity<>(request, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + student.getId() + "/responsibility",
            HttpMethod.PATCH,
            entity,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((String) documentContext.read("$.message"))
        .isEqualTo("Student responsibility updated successfully");
    assertThat((String) documentContext.read("$.data.responsibility"))
        .isEqualTo("CLASS_REPRESENTATIVE");

    Student updated = studentRepository.findById(student.getId()).orElseThrow();
    assertThat(updated.getResponsibility()).isEqualTo(Responsibility.CLASS_REPRESENTATIVE);
  }

  @Test
  @DisplayName(
      "PATCH /students/{id}/responsibility - Should return 403 Forbidden when standard STUDENT attempts to update responsibility")
  void shouldReturnForbiddenWhenStudentAttemptsToUpdateResponsibility() {
    Student caller = createDefaultStudent();
    Student target =
        createTestStudent("Target", "target@example.com", "Secret123!", Responsibility.STUDENT);

    String token = createAccessToken(caller);
    HttpHeaders headers = createBearerHeaders(token);
    headers.setContentType(MediaType.APPLICATION_JSON);

    UpdateResponsibilityRequest request =
        new UpdateResponsibilityRequest(Responsibility.CLASS_REPRESENTATIVE);
    HttpEntity<UpdateResponsibilityRequest> entity = new HttpEntity<>(request, headers);

    ResponseEntity<String> response =
        testRestTemplate.exchange(
            "/api/v1/students/" + target.getId() + "/responsibility",
            HttpMethod.PATCH,
            entity,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(response.getBody());
    assertThat((Integer) documentContext.read("$.status")).isEqualTo(403);
    assertThat((String) documentContext.read("$.code")).isEqualTo("FORBIDDEN");
  }

  @Test
  @DisplayName(
      "PATCH /students/{id}/responsibility - Demoting a CLASS_REPRESENTATIVE takes effect upon issuing subsequent access tokens")
  void shouldReflectDemotionOnNextIssuedAccessTokenWhenClassRepresentativeIsDemoted() {
    Student admin = createClassRepresentative();
    Student targetRep =
        createTestStudent(
            "Target Rep",
            "target.rep@example.com",
            "Password123!",
            Responsibility.CLASS_REPRESENTATIVE);

    // 1. Issue access token while targetRep is CLASS_REPRESENTATIVE
    String targetRepToken = createAccessToken(targetRep);
    HttpHeaders targetRepHeaders = createBearerHeaders(targetRepToken);

    // 2. Target rep initially has access to CR-only endpoint (GET /students)
    ResponseEntity<String> initialResponse =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(targetRepHeaders), String.class);
    assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 3. Admin demotes targetRep to standard STUDENT
    String adminToken = createAccessToken(admin);
    HttpHeaders adminHeaders = createBearerHeaders(adminToken);
    adminHeaders.setContentType(MediaType.APPLICATION_JSON);
    UpdateResponsibilityRequest demoteRequest =
        new UpdateResponsibilityRequest(Responsibility.STUDENT);
    HttpEntity<UpdateResponsibilityRequest> demoteEntity =
        new HttpEntity<>(demoteRequest, adminHeaders);

    ResponseEntity<String> demoteResponse =
        testRestTemplate.exchange(
            "/api/v1/students/" + targetRep.getId() + "/responsibility",
            HttpMethod.PATCH,
            demoteEntity,
            String.class);
    assertThat(demoteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 4. Existing token remains valid during its short-lived lifespan (stateless JWT)
    ResponseEntity<String> existingTokenResponse =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(targetRepHeaders), String.class);
    assertThat(existingTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 5. Subsequent access token issued after demotion reflects the new STUDENT role
    Student updatedTarget = studentRepository.findById(targetRep.getId()).orElseThrow();
    String newToken = createAccessToken(updatedTarget);
    HttpHeaders newTokenHeaders = createBearerHeaders(newToken);

    ResponseEntity<String> subsequentResponse =
        testRestTemplate.exchange(
            "/api/v1/students", HttpMethod.GET, new HttpEntity<>(newTokenHeaders), String.class);

    // Access is denied with 403 Forbidden because newly issued token has ROLE_STUDENT
    assertThat(subsequentResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    DocumentContext documentContext = JsonPath.parse(subsequentResponse.getBody());
    assertThat((String) documentContext.read("$.code")).isEqualTo("FORBIDDEN");
  }
}
