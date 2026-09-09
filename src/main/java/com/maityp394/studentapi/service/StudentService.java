package com.maityp394.studentapi.service;

import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Responsibility;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Service interface defining business logic operations for managing students. */
public interface StudentService {

  /**
   * Retrieves a student by their unique ID.
   *
   * @param id the unique ID of the student
   * @return the {@link StudentResponse} matching the ID
   * @throws com.maityp394.studentapi.exception.ResourceNotFoundException if no student exists with
   *     the given ID
   */
  StudentResponse getStudentById(UUID id);

  /**
   * Retrieves a paginated and sorted list of students, optionally filtered by responsibility.
   *
   * @param responsibility optional responsibility filter (if null, returns all students)
   * @param pageable pagination and sorting parameters
   * @return a list of {@link StudentResponse} items for the requested page
   */
  List<StudentResponse> getAllStudents(Responsibility responsibility, Pageable pageable);

  /**
   * Fully replaces an existing student's details.
   *
   * @param id the unique ID of the student to update
   * @param request the request containing full updated details
   * @return the updated {@link StudentResponse}
   * @throws com.maityp394.studentapi.exception.ResourceNotFoundException if no student exists with
   *     the given ID
   */
  StudentResponse updateStudent(UUID id, UpdateStudentRequest request);

  /**
   * Partially updates an existing student's attributes with non-null values.
   *
   * @param id the unique ID of the student to patch
   * @param request the request containing the partial update attributes
   * @return the patched {@link StudentResponse}
   * @throws com.maityp394.studentapi.exception.ResourceNotFoundException if no student exists with
   *     the given ID
   */
  StudentResponse patchStudent(UUID id, PatchStudentRequest request);

  /**
   * Deletes a student by their unique ID.
   *
   * @param id the unique ID of the student to delete
   */
  void deleteStudent(UUID id);

  /**
   * Updates a student's institutional responsibility/role.
   *
   * @param id the unique ID of the student
   * @param responsibility the new responsibility role to assign
   * @return the updated {@link StudentResponse}
   * @throws com.maityp394.studentapi.exception.ResourceNotFoundException if no student exists with
   *     the given ID
   */
  StudentResponse updateResponsibility(UUID id, Responsibility responsibility);
}
