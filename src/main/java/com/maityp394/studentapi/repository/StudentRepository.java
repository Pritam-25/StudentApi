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

  Optional<Student> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, UUID id);

  Page<Student> findAllByResponsibility(Responsibility responsibility, Pageable pageable);
}
