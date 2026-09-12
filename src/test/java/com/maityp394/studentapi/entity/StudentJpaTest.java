package com.maityp394.studentapi.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.maityp394.studentapi.integration.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Student JPA and Proxy Equality Tests")
class StudentJpaTest extends BaseIntegrationTest {

  @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;

  @Test
  @DisplayName("equals and hashCode should support proxy-to-entity and entity-to-proxy equality")
  void shouldSupportProxyAndEntityEqualityBothDirections() {
    new org.springframework.transaction.support.TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              Student student =
                  createTestStudent(
                      "Pritam", "pritam.jpa@example.com", "Secret123!", Responsibility.STUDENT);
              UUID studentId = student.getId();

              // Obtain a lazy proxy reference via Spring Data JPA getReferenceById
              Student proxy = studentRepository.getReferenceById(studentId);

              // Verify entity-to-proxy equality in both directions
              assertThat(student).isEqualTo(proxy);
              assertThat(proxy).isEqualTo(student);

              // Verify consistent hash codes
              assertThat(student.hashCode()).isEqualTo(proxy.hashCode());
              assertThat(student.hashCode()).isEqualTo(Student.class.hashCode());
              assertThat(proxy.hashCode()).isEqualTo(Student.class.hashCode());
            });
  }

  @Test
  @DisplayName("equals should obey standard contract for null, transient, and different entities")
  void shouldObeyStandardEqualsContract() {
    Student transient1 = new Student("A", "a@example.com", "hash", Responsibility.STUDENT);
    Student transient2 = new Student("B", "b@example.com", "hash", Responsibility.STUDENT);

    // Transient entities without ID are never equal
    assertThat(transient1).isNotEqualTo(transient2);
    assertThat(transient1).isNotEqualTo(null);
    assertThat(transient1).isNotEqualTo("some string");

    // Same instance is equal to itself
    assertThat(transient1).isEqualTo(transient1);

    // Persisted entities with different IDs are not equal
    Student p1 = createTestStudent("A", "a@example.com", "Secret123!", Responsibility.STUDENT);
    Student p2 = createTestStudent("B", "b@example.com", "Secret123!", Responsibility.STUDENT);

    assertThat(p1).isNotEqualTo(p2);
    assertThat(p2).isNotEqualTo(p1);
  }
}
