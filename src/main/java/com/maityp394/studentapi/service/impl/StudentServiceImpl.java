package com.maityp394.studentapi.service.impl;

import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.PageResponse;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.DuplicateResourceException;
import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.ResourceNotFoundException;
import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.service.StudentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link StudentService} providing business operations for student management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

  private final StudentRepository studentRepository;
  private final StudentMapper studentMapper;

  /** {@inheritDoc} */
  @Override
  public StudentResponse getStudentById(UUID id) {
    Student student = findStudentByIdOrThrow(id);
    log.info("Student fetched successfully: id={}", id);
    return studentMapper.toResponse(student);
  }

  /** {@inheritDoc} */
  @Override
  public PageResponse<StudentResponse> getAllStudents(
      Responsibility responsibility, Pageable pageable) {
    PageRequest pageRequest =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSortOr(Sort.by(Sort.Direction.DESC, "name")));

    Page<Student> page =
        (responsibility != null)
            ? studentRepository.findAllByResponsibility(responsibility, pageRequest)
            : studentRepository.findAll(pageRequest);

    log.info("Students fetched successfully: count={}", page.getNumberOfElements());
    return PageResponse.from(page.map(studentMapper::toResponse));
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  @CacheEvict(cacheNames = "student-profile", key = "#id")
  public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
    Student student = findStudentByIdOrThrow(id);
    String email = request.email();

    if (studentRepository.existsByEmailAndIdNot(email, id)) {
      throw new DuplicateResourceException(
          ErrorCode.STUDENT_EMAIL_ALREADY_EXISTS, "Student already exists with email: " + email);
    }

    student.updateProfile(request.name(), email);

    Student updatedStudent = studentRepository.saveAndFlush(student);
    log.info("Student updated successfully with id: {}", id);
    return studentMapper.toResponse(updatedStudent);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  @CacheEvict(cacheNames = "student-profile", key = "#id")
  public StudentResponse patchStudent(UUID id, PatchStudentRequest request) {
    Student student = findStudentByIdOrThrow(id);

    if (request.email() != null && !request.email().isBlank()) {
      String email = request.email();
      if (studentRepository.existsByEmailAndIdNot(email, id)) {
        throw new DuplicateResourceException(
            ErrorCode.STUDENT_EMAIL_ALREADY_EXISTS, "Student already exists with email: " + email);
      }
      student.setEmail(email);
    }

    if (request.name() != null && !request.name().isBlank()) {
      student.setName(request.name());
    }

    Student updatedStudent = studentRepository.saveAndFlush(student);
    log.info("Student patched successfully with id: {}", id);
    return studentMapper.toResponse(updatedStudent);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  @CacheEvict(cacheNames = "student-profile", key = "#id")
  public void deleteStudent(UUID id) {
    Student student = findStudentByIdOrThrow(id);
    studentRepository.delete(student);
    log.info("Student deleted successfully with id: {}", id);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  @CacheEvict(cacheNames = "student-profile", key = "#id")
  public StudentResponse updateResponsibility(UUID id, Responsibility responsibility) {
    Student student = findStudentByIdOrThrow(id);
    student.assignResponsibility(responsibility);
    Student saved = studentRepository.saveAndFlush(student);
    log.info(
        "Student responsibility updated successfully: id={}, responsibility={}",
        id,
        responsibility);
    return studentMapper.toResponse(saved);
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
