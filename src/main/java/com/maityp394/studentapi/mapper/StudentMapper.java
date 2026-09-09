package com.maityp394.studentapi.mapper;

import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import org.springframework.stereotype.Component;

/** Component responsible for mapping between student entity and student DTOs. */
@Component
public class StudentMapper {

  /**
   * Converts a {@link RegisterRequest} DTO and an encoded password hash to a new {@link Student}
   * entity.
   *
   * @param req the registration request DTO
   * @param passwordHash the cryptographically hashed password
   * @return a newly populated {@link Student} entity with the hashed password
   */
  public Student toEntity(RegisterRequest req, String passwordHash) {
    Student student = new Student();
    student.setName(req.name());
    student.setEmail(req.email());
    student.setPasswordHash(passwordHash);
    student.setResponsibility(Responsibility.STUDENT);
    return student;
  }

  /**
   * Converts a {@link Student} entity to a {@link StudentResponse} DTO.
   *
   * @param student the student entity from the database
   * @return a {@link StudentResponse} containing the formatted student details
   */
  public StudentResponse toResponse(Student student) {
    return new StudentResponse(
        student.getId(),
        student.getName(),
        student.getEmail(),
        student.getResponsibility(),
        student.getCreatedAt(),
        student.getUpdatedAt());
  }
}
