package com.maityp394.studentapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Entity representing a student record in the database. */
@Getter
@Setter
@ToString(exclude = "passwordHash")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "students",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_student_email", columnNames = "email"),
      @UniqueConstraint(name = "uk_students_google_subject", columnNames = "google_subject")
    },
    indexes = {
      @Index(name = "idx_students_responsibility", columnList = "responsibility"),
      @Index(name = "idx_students_name", columnList = "name"),
      @Index(name = "idx_students_google_subject", columnList = "google_subject")
    })
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

  /** BCrypt hash of the student's password. Nullable for accounts authenticated via OAuth2. */
  @Column(name = "password_hash")
  private String passwordHash;

  /** Google Subject identifier (subclaim) for OAuth2/OIDC linked accounts. */
  @Column(name = "google_subject", unique = true)
  private String googleSubject;

  /** The role or responsibility of the student. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Responsibility responsibility;

  /** UTC timestamp indicating when the student record was created. */
  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  @Setter(AccessLevel.NONE)
  private Instant createdAt;

  /** UTC timestamp indicating when the student record was last updated. */
  @UpdateTimestamp
  @Column(nullable = false)
  @Setter(AccessLevel.NONE)
  private Instant updatedAt;

  /**
   * Constructs a new {@link Student} entity with local password credentials.
   *
   * @param name Name of the student.
   * @param email Email of the student.
   * @param passwordHash BCrypt hash of the student's password.
   * @param responsibility The role or responsibility of the student.
   */
  public Student(String name, String email, String passwordHash, Responsibility responsibility) {
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.responsibility = responsibility;
  }

  /**
   * Constructs a new {@link Student} entity with Google OAuth credentials.
   *
   * @param name Name of the student.
   * @param email Email of the student.
   * @param responsibility The role or responsibility of the student.
   * @param googleSubject Unique Google Subject identifier (subclaim).
   */
  public Student(String name, String email, Responsibility responsibility, String googleSubject) {
    this.name = name;
    this.email = email;
    this.responsibility = responsibility;
    this.googleSubject = googleSubject;
  }

  /**
   * Links a Google OAuth account by setting its unique subject identifier.
   *
   * @param googleSubject the Google sub identifier
   */
  public void linkGoogleAccount(String googleSubject) {
    this.googleSubject = googleSubject;
  }

  /** Unlinks the associated Google OAuth account. */
  public void unlinkGoogleAccount() {
    this.googleSubject = null;
  }

  /**
   * Checks whether this student account is linked to a Google OAuth identity.
   *
   * @return {@code true} if linked to Google, {@code false} otherwise
   */
  public boolean isGoogleLinked() {
    return this.googleSubject != null;
  }

  /**
   * Updates the student profile name and email address.
   *
   * @param name new name
   * @param email new email address
   */
  public void updateProfile(String name, String email) {
    this.name = name;
    this.email = email;
  }

  /**
   * Updates the institutional responsibility of the student.
   *
   * @param responsibility new responsibility role
   */
  public void assignResponsibility(Responsibility responsibility) {
    this.responsibility = responsibility;
  }

  /**
   * Evaluates entity equality based on database identity ({@code id}).
   *
   * <p>Transient entities without an assigned {@code id} are never considered equal to other
   * instances. Comparing solely by primary key ensures consistency across different Hibernate
   * sessions and detached states, regardless of mutable field modifications.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Student student)) {
      return false;
    }
    return id != null && id.equals(student.getId());
  }

  /**
   * Returns a fixed class-level hash code.
   *
   * <p>Guarantees that the hash code remains constant across all JPA lifecycle transitions
   * (transient, persisted, detached) and between entities and Hibernate proxies.
   */
  @Override
  public int hashCode() {
    return Student.class.hashCode();
  }
}
