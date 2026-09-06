package com.maityp394.studentapi.repository;

import com.maityp394.studentapi.entity.Student;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing persistence and query operations for {@link Student} entities.
 */
public interface StudentRepository extends JpaRepository<Student, UUID> {

  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, UUID id);
}
