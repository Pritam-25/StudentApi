package com.maityp394.studentapi.dto.response;

import java.util.UUID;
import lombok.Data;

/** Data transfer object representing the response payload containing student details. */
@Data
public class StudentResponse {
  /** The unique identifier of the student. */
  private UUID id;

  /** The full name of the student. */
  private String name;

  /** The email address of the student. */
  private String email;

  /**
   * Constructs a new {@code StudentResponse} with all field values.
   *
   * @param id the unique student ID
   * @param name the student's full name
   * @param email the student's email address
   */
  public StudentResponse(UUID id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
  }
}
