package com.maityp394.studentapi.controller;

import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateResponsibilityRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.ApiResponse;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.exception.ForbiddenException;
import com.maityp394.studentapi.security.IsClassRepresentative;
import com.maityp394.studentapi.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller providing CRUD and partial update endpoints for managing students.
 *
 * <p>Exposes operations under the {@code /api/v1/students} base path.
 */
@RestController
@RequestMapping("/api/v1/students")
@Tag(
    name = "Students",
    description = "Student resource CRUD and responsibility management endpoints")
public class StudentController {

  private final StudentService studentService;

  /**
   * Constructs a new {@code StudentController} with the required {@link StudentService}.
   *
   * @param studentService the service handling student business logic
   */
  public StudentController(StudentService studentService) {
    this.studentService = studentService;
  }

  /**
   * Retrieves a paginated list of students, optionally filtered by responsibility. Only accessible
   * by Class Representatives.
   *
   * @param responsibility optional responsibility filter (e.g. STUDENT or CLASS_REPRESENTATIVE)
   * @param pageable pagination and sorting parameters (defaults to 5 items per page sorted by name)
   * @return a {@link ResponseEntity} containing a list of {@link StudentResponse} objects
   */
  @GetMapping
  @IsClassRepresentative
  @Operation(summary = "Get paginated list of students (Class Representative only)")
  public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
      @RequestParam(required = false) Responsibility responsibility,
      @PageableDefault(size = 5, sort = "name") Pageable pageable) {

    List<StudentResponse> students = studentService.getAllStudents(responsibility, pageable);

    return ResponseEntity.ok(new ApiResponse<>("Students fetched successfully", students));
  }

  /**
   * Retrieves a specific student by their unique identifier.
   *
   * @param id the unique ID of the student
   * @return a {@link ResponseEntity} containing the {@link StudentResponse}
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get student by ID")
  public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable UUID id) {

    return ResponseEntity.ok(
        new ApiResponse<>("Student fetched successfully", studentService.getStudentById(id)));
  }

  /**
   * Fully updates an existing student's details.
   *
   * @param id the unique ID of the student to update
   * @param request the updated student payload
   * @return a {@link ResponseEntity} containing the updated {@link StudentResponse}
   */
  @PutMapping("/{id}")
  @Operation(summary = "Fully update student information")
  public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateStudentRequest request,
      @AuthenticationPrincipal Jwt jwt) {

    validateResourceOwner(id, jwt);
    StudentResponse updatedStudent = studentService.updateStudent(id, request);

    return ResponseEntity.ok(new ApiResponse<>("Student updated successfully", updatedStudent));
  }

  /**
   * Partially updates an existing student's information.
   *
   * @param id the unique ID of the student to partially update
   * @param request the request containing non-null fields to be updated
   * @param jwt the validated JWT principal of the authenticated caller
   * @return a {@link ResponseEntity} containing the partially updated {@link StudentResponse}
   */
  @PatchMapping("/{id}")
  @Operation(summary = "Partially update student information")
  public ResponseEntity<ApiResponse<StudentResponse>> patchStudent(
      @PathVariable UUID id,
      @Valid @RequestBody PatchStudentRequest request,
      @AuthenticationPrincipal Jwt jwt) {

    validateResourceOwner(id, jwt);
    StudentResponse updatedStudent = studentService.patchStudent(id, request);

    return ResponseEntity.ok(new ApiResponse<>("Student updated successfully", updatedStudent));
  }

  /**
   * Deletes a student by their unique identifier.
   *
   * @param id the unique ID of the student to delete
   * @param jwt the validated JWT principal of the authenticated caller
   * @return a {@link ResponseEntity} with status {@code 204 No Content} and an empty response body
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete student by ID")
  public ResponseEntity<Void> deleteStudent(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    validateResourceOwner(id, jwt);
    studentService.deleteStudent(id);

    return ResponseEntity.noContent().build();
  }

  /**
   * Updates an existing student's institutional responsibility. Only accessible by Class
   * Representatives.
   *
   * @param id the unique ID of the student
   * @param request the request containing the new responsibility
   * @return a {@link ResponseEntity} containing the updated {@link StudentResponse}
   */
  @PatchMapping("/{id}/responsibility")
  @IsClassRepresentative
  @Operation(summary = "Update student responsibility (Class Representative only)")
  public ResponseEntity<ApiResponse<StudentResponse>> updateResponsibility(
      @PathVariable UUID id, @Valid @RequestBody UpdateResponsibilityRequest request) {

    StudentResponse updatedStudent =
        studentService.updateResponsibility(id, request.responsibility());

    return ResponseEntity.ok(
        new ApiResponse<>("Student responsibility updated successfully", updatedStudent));
  }

  /**
   * Verifies that the authenticated caller owns the resource they are attempting to mutate.
   *
   * @param resourceId the ID of the student resource being modified
   * @param jwt the validated JWT principal of the caller
   * @throws ForbiddenException if the caller ID does not match the resource ID
   */
  private void validateResourceOwner(UUID resourceId, Jwt jwt) {
    if (jwt == null || !resourceId.toString().equals(jwt.getSubject())) {
      throw new ForbiddenException("You do not have permission to modify this student account");
    }
  }
}
