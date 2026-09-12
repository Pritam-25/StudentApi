package com.maityp394.studentapi.repository;

import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing persistence and query operations for {@link Student} entities.
 */
public interface StudentRepository extends JpaRepository<Student, UUID> {

  /**
   * Retrieves a student entity by email address.
   *
   * @param email the email address to search for
   * @return an {@link Optional} containing the matched {@link Student}, or empty if not found
   */
  Optional<Student> findByEmail(String email);

  /**
   * Checks whether a student record with the given email address already exists.
   *
   * @param email the email address to verify
   * @return {@code true} if a record exists with the email, {@code false} otherwise
   */
  boolean existsByEmail(String email);

  /**
   * Checks whether another student record (excluding the given ID) uses the specified email.
   *
   * @param email the email address to check
   * @param id the student ID to exclude from collision checks
   * @return {@code true} if another record with the email exists, {@code false} otherwise
   */
  boolean existsByEmailAndIdNot(String email, UUID id);

  /**
   * Retrieves a paginated list of students filtered by their institutional responsibility.
   *
   * @param responsibility the responsibility to filter by
   * @param pageable pagination and sorting parameters
   * @return a {@link Page} of matched {@link Student} entities
   */
  Page<Student> findAllByResponsibility(Responsibility responsibility, Pageable pageable);
}
