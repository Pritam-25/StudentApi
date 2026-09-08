package com.maityp394.studentapi.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Entity representing a student record in the database. */
@Entity
@Table(
    name = "students",
    uniqueConstraints = {@UniqueConstraint(name = "uk_student_email", columnNames = "email")},
    indexes = {
      @Index(name = "idx_students_responsibility", columnList = "responsibility"),
      @Index(name = "idx_students_name", columnList = "name")
    })
@Getter
@Setter
public class Student {

  /** Unique identifier for the student, generated as a UUID. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** The full name of the student. */
  @Column(nullable = false, length = 50)
  private String name;

  /** The unique email address of the student. */
  @Column(unique = true, nullable = false)
  private String email;

  /** BCrypt hash of the student's password. Never stores plaintext. */
  @Column(nullable = false)
  private String passwordHash;

  /** The role or responsibility of the student. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Responsibility responsibility;

  /** UTC timestamp indicating when the student record was created. */
  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  /** UTC timestamp indicating when the student record was last updated. */
  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;
}
