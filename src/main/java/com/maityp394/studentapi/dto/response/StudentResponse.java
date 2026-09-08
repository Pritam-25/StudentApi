package com.maityp394.studentapi.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.maityp394.studentapi.entity.Responsibility;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data transfer object representing the response payload containing student details. */
@Data
@NoArgsConstructor
@JsonPropertyOrder({"id", "name", "email", "responsibility", "createdAt", "updatedAt"})
public class StudentResponse {
  /** The unique identifier of the student. */
  private UUID id;

  /** The full name of the student. */
  private String name;

  /** The email address of the student. */
  private String email;

  /** The institutional responsibility/role of the student. */
  private Responsibility responsibility;

  /** UTC timestamp indicating when the student account was created. */
  private Instant createdAt;

  /** UTC timestamp indicating when the student profile was last updated. */
  private Instant updatedAt;

  /**
   * Constructs a new {@code StudentResponse} with all field values including audit timestamps.
   *
   * @param id the unique student ID
   * @param name the student's full name
   * @param email the student's email address
   * @param responsibility the student's responsibility role
   * @param createdAt UTC creation timestamp
   * @param updatedAt UTC last update timestamp
   */
  public StudentResponse(
      UUID id,
      String name,
      String email,
      Responsibility responsibility,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.responsibility = responsibility;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
