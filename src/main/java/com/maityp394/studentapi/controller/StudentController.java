package com.maityp394.studentapi.controller;

import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.ApiResponse;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.service.StudentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller providing CRUD and partial update endpoints for managing students.
 *
 * <p>Exposes operations under the {@code /api/v1/students} base path.
 */
@RestController
@RequestMapping("/api/v1/students")
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
   * Creates a new student.
   *
   * @param request the student creation request containing student details
   * @param ucbBuilder URI builder used to compose the Location header
   * @return a {@link ResponseEntity} containing the created {@link StudentResponse} and a 201
   *     Created status
   */
  @PostMapping
  public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
      @Valid @RequestBody CreateStudentRequest request, UriComponentsBuilder ucbBuilder) {

    StudentResponse createdStudent = studentService.createStudent(request);

    URI location =
        ucbBuilder.path("/api/v1/students/{id}").buildAndExpand(createdStudent.getId()).toUri();

    return ResponseEntity.created(location)
        .body(new ApiResponse<>("Student created successfully", createdStudent));
  }

  /**
   * Retrieves a paginated list of students.
   *
   * @param pageable pagination and sorting parameters (defaults to 5 items per page sorted by name)
   * @return a {@link ResponseEntity} containing a list of {@link StudentResponse} objects
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
      @PageableDefault(size = 5, sort = "name") Pageable pageable) {

    List<StudentResponse> students = studentService.getAllStudents(pageable);

    return ResponseEntity.ok(new ApiResponse<>("Students fetched successfully", students));
  }

  /**
   * Retrieves a specific student by their unique identifier.
   *
   * @param id the unique ID of the student
   * @return a {@link ResponseEntity} containing the {@link StudentResponse}
   */
  @GetMapping("/{id}")
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
  public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
      @PathVariable UUID id, @Valid @RequestBody UpdateStudentRequest request) {

    StudentResponse updatedStudent = studentService.updateStudent(id, request);

    return ResponseEntity.ok(new ApiResponse<>("Student updated successfully", updatedStudent));
  }

  /**
   * Partially updates an existing student's information.
   *
   * @param id the unique ID of the student to partially update
   * @param request the request containing non-null fields to be updated
   * @return a {@link ResponseEntity} containing the partially updated {@link StudentResponse}
   */
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<StudentResponse>> patchStudent(
      @PathVariable UUID id, @Valid @RequestBody PatchStudentRequest request) {

    StudentResponse updatedStudent = studentService.patchStudent(id, request);

    return ResponseEntity.ok(new ApiResponse<>("Student partially updated", updatedStudent));
  }

  /**
   * Deletes a student by their unique identifier.
   *
   * @param id the unique ID of the student to delete
   * @return a {@link ResponseEntity} with status {@code 204 No Content} and an empty response body
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteStudent(@PathVariable UUID id) {

    studentService.deleteStudent(id);

    return ResponseEntity.noContent().build();
  }
}
