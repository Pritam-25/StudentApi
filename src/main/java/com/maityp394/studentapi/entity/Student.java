package com.maityp394.studentapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Entity representing a student record in the database. */
@Entity
@Table(
    name = "students",
    uniqueConstraints = {@UniqueConstraint(name = "uk_student_email", columnNames = "email")})
@Getter
@Setter
public class Student {

  /** Unique identifier for the student, generated as a UUID. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** The full name of the student. */
  private String name;

  /** The unique email address of the student. */
  @Column(unique = true, nullable = false)
  private String email;

  /** The encrypted password for the student account. */
  private String password;

  /** The role or responsibility of the student. */
  @Enumerated(EnumType.STRING)
  private Responsibility responsibility;

  /** Timestamp indicating when the student record was created. */
  private LocalDateTime createdAt;

  /** Timestamp indicating when the student record was last updated. */
  private LocalDateTime updatedAt;

  /**
   * Lifecycle callback executed before the entity is first persisted to set creation and update
   * timestamps.
   */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now(ZoneId.systemDefault());
    updatedAt = LocalDateTime.now(ZoneId.systemDefault());
  }

  /**
   * Lifecycle callback executed before an existing entity is updated to refresh the updated
   * timestamp.
   */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now(ZoneId.systemDefault());
  }
}
