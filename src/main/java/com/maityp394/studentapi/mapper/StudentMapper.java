package com.maityp394.studentapi.mapper;

import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Student;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Component responsible for mapping between student entity and student DTOs. */
@Component
public class StudentMapper {

  private final PasswordEncoder passwordEncoder;

  /**
   * Constructs a new {@link StudentMapper} with the required {@link PasswordEncoder}.
   *
   * @param passwordEncoder the encoder used to hash sensitive credentials
   */
  public StudentMapper(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Converts a {@link CreateStudentRequest} DTO to a new {@link Student} entity, hashing the raw
   * password using BCrypt before persisting to prevent plaintext password leakage.
   *
   * @param req the student creation request DTO
   * @return a newly populated {@link Student} entity with a hashed password
   */
  public Student toEntity(CreateStudentRequest req) {
    Student student = new Student();
    student.setName(req.name());
    student.setEmail(req.email());
    student.setPassword(passwordEncoder.encode(req.password()));
    return student;
  }

  /**
   * Converts a {@link Student} entity to a {@link StudentResponse} DTO.
   *
   * @param student the student entity from the database
   * @return a {@link StudentResponse} containing the formatted student details
   */
  public StudentResponse toResponse(Student student) {
    return new StudentResponse(student.getId(), student.getName(), student.getEmail());
  }
}
