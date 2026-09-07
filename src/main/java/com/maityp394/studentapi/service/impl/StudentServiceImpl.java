package com.maityp394.studentapi.service.impl;

import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.DuplicateResourceException;
import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.ResourceNotFoundException;
import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.service.StudentService;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link StudentService} providing business operations for student management.
 */
@Slf4j
@Service
// @RequiredArgsConstructor -> generate constructor by default
public class StudentServiceImpl implements StudentService {

  private final StudentRepository studentRepository;
  private final StudentMapper studentMapper;

  /**
   * Constructs a new {@code StudentServiceImpl} with required dependencies.
   *
   * @param studentRepository the repository for database access
   * @param studentMapper the mapper for converting between entity and DTO representations
   */
  public StudentServiceImpl(StudentRepository studentRepository, StudentMapper studentMapper) {
    this.studentRepository = studentRepository;
    this.studentMapper = studentMapper;
  }

  /** {@inheritDoc} */
  @Override
  public StudentResponse getStudentById(UUID id) {
    Student student = findStudentByIdOrThrow(id);
    log.info("Student fetched successfully: id={}", id);
    return studentMapper.toResponse(student);
  }

  /** {@inheritDoc} */
  @Override
  public List<StudentResponse> getAllStudents(Pageable pageable) {
    Page<Student> page =
        studentRepository.findAll(
            PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "name"))));

    log.info("Students fetched successfully");
    return page.map(studentMapper::toResponse).getContent();
  }

  /** {@inheritDoc} */
  @Override
  public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
    Student student = findStudentByIdOrThrow(id);

    if (studentRepository.existsByEmailAndIdNot(request.email(), id)) {
      throw new DuplicateResourceException(
          ErrorCode.STUDENT_EMAIL_ALREADY_EXISTS,
          "Student already exists with email: " + request.email());
    }

    student.setName(request.name());
    student.setEmail(request.email());

    Student updatedStudent = studentRepository.save(student);
    log.info("Student updated successfully with id: {}", id);
    return studentMapper.toResponse(updatedStudent);
  }

  /** {@inheritDoc} */
  @Override
  public StudentResponse patchStudent(UUID id, PatchStudentRequest request) {
    Student student = findStudentByIdOrThrow(id);

    if (request.email() != null && studentRepository.existsByEmailAndIdNot(request.email(), id)) {
      throw new DuplicateResourceException(
          ErrorCode.STUDENT_EMAIL_ALREADY_EXISTS,
          "Student already exists with email: " + request.email());
    }

    // PARTIAL UPDATE (only non-null fields)
    if (request.name() != null) {
      student.setName(request.name());
    }

    if (request.email() != null) {
      student.setEmail(request.email());
    }

    Student updatedStudent = studentRepository.save(student);
    log.info("Student patched successfully with id: {}", id);
    return studentMapper.toResponse(updatedStudent);
  }

  /** {@inheritDoc} */
  @Override
  public void deleteStudent(UUID id) {
    Student student = findStudentByIdOrThrow(id);
    studentRepository.delete(student);
    log.info("Student deleted successfully with id: {}", id);
  }

  /**
   * Retrieves an existing {@link Student} by id or throws a {@link ResourceNotFoundException}.
   *
   * @param id the unique ID of the student
   * @return the resolved {@link Student} entity
   * @throws ResourceNotFoundException if no student with the specified id exists
   */
  private Student findStudentByIdOrThrow(UUID id) {
    return studentRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ErrorCode.STUDENT_NOT_FOUND, "Student not found: " + id));
  }
}
