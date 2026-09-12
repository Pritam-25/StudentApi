package com.maityp394.studentapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.session.RedisSessionService;
import com.maityp394.studentapi.security.token.TokenService;
import com.maityp394.studentapi.security.user.StudentPrincipal;
import com.maityp394.studentapi.service.impl.AuthServiceImpl;
import com.maityp394.studentapi.service.impl.StudentServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Isolated unit/service-level test verifying Spring Cache abstraction behavior for student profile
 * caching and eviction mechanics.
 */
@SpringJUnitConfig(StudentProfileCacheTest.TestCacheConfig.class)
class StudentProfileCacheTest {

  @Configuration
  @EnableCaching
  static class TestCacheConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("student-profile");
    }

    @Bean
    StudentMapper studentMapper() {
      return new StudentMapper();
    }

    @Bean
    StudentRepository studentRepository() {
      return mock(StudentRepository.class);
    }

    @Bean
    AuthService authService(StudentRepository studentRepository, StudentMapper studentMapper) {
      return new AuthServiceImpl(
          studentRepository,
          mock(AuthenticationManager.class),
          mock(PasswordEncoder.class),
          studentMapper,
          mock(TokenService.class),
          mock(RedisSessionService.class));
    }

    @Bean
    StudentService studentService(
        StudentRepository studentRepository, StudentMapper studentMapper) {
      return new StudentServiceImpl(studentRepository, studentMapper);
    }
  }

  @Autowired private AuthService authService;
  @Autowired private StudentService studentService;
  @Autowired private StudentRepository studentRepository;
  @Autowired private CacheManager cacheManager;

  private UUID studentId;
  private Student student;

  @BeforeEach
  void setUp() {
    var cache = cacheManager.getCache("student-profile");
    if (cache != null) {
      cache.clear();
    }
    org.mockito.Mockito.reset(studentRepository);

    studentId = UUID.randomUUID();
    student = new Student("Pritam Maity", "pritam@example.com", "hash", Responsibility.STUDENT);
    student.setId(studentId);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager.clear();
    }
  }

  @Test
  @DisplayName("Calling getCurrentUser multiple times queries database only once (Cache Hit)")
  void getCurrentUser_WhenCalledMultipleTimes_HitsDatabaseOnlyOnce() {
    StudentResponse first = authService.getCurrentUser(studentId);
    StudentResponse second = authService.getCurrentUser(studentId);

    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(first.id()).isEqualTo(studentId);
    assertThat(second.id()).isEqualTo(studentId);

    // Repository findById must be called exactly once
    verify(studentRepository, times(1)).findById(studentId);
  }

  @Test
  @DisplayName("updateStudent evicts student-profile cache entry")
  void updateStudent_EvictsCachedProfile() {
    var cache = cacheManager.getCache("student-profile");
    assertThat(cache).isNotNull();

    // 1. Populate cache
    authService.getCurrentUser(studentId);
    assertThat(cache.get(studentId)).isNotNull();
    verify(studentRepository, times(1)).findById(studentId);

    // 2. Perform update (evicts cache)
    UpdateStudentRequest updateReq =
        new UpdateStudentRequest("Updated Name", "updated@example.com");
    studentService.updateStudent(studentId, updateReq);

    // 3. Cache entry must be evicted
    assertThat(cache.get(studentId)).isNull();

    // 4. Next call to getCurrentUser reloads from repository
    authService.getCurrentUser(studentId);
    assertThat(cache.get(studentId)).isNotNull();
    verify(studentRepository, times(3)).findById(studentId);
  }

  @Test
  @DisplayName("patchStudent evicts student-profile cache entry")
  void patchStudent_EvictsCachedProfile() {
    var cache = cacheManager.getCache("student-profile");
    assertThat(cache).isNotNull();

    // 1. Populate cache
    authService.getCurrentUser(studentId);
    assertThat(cache.get(studentId)).isNotNull();
    verify(studentRepository, times(1)).findById(studentId);

    // 2. Perform patch (evicts cache)
    PatchStudentRequest patchReq = new PatchStudentRequest("Patched Name", null);
    studentService.patchStudent(studentId, patchReq);

    // 3. Cache entry must be evicted
    assertThat(cache.get(studentId)).isNull();

    // 4. Next call to getCurrentUser reloads from repository
    authService.getCurrentUser(studentId);
    assertThat(cache.get(studentId)).isNotNull();
    verify(studentRepository, times(3)).findById(studentId);
  }

  @Test
  @DisplayName("updateResponsibility evicts student-profile cache entry")
  void updateResponsibility_EvictsCachedProfile() {
    var cache = cacheManager.getCache("student-profile");
    assertThat(cache).isNotNull();

    // 1. Populate cache
    authService.getCurrentUser(studentId);
    assertThat(cache.get(studentId)).isNotNull();
    verify(studentRepository, times(1)).findById(studentId);

    // 2. Update responsibility (evicts cache)
    studentService.updateResponsibility(studentId, Responsibility.CLASS_REPRESENTATIVE);

    // 3. Cache entry must be evicted
    assertThat(cache.get(studentId)).isNull();

    // 4. Next call to getCurrentUser reloads from repository
    authService.getCurrentUser(studentId);
    assertThat(cache.get(studentId)).isNotNull();
    verify(studentRepository, times(3)).findById(studentId);
  }

  @Test
  @DisplayName("deleteStudent evicts student-profile cache entry")
  void deleteStudent_EvictsCachedProfile() {
    var cache = cacheManager.getCache("student-profile");
    assertThat(cache).isNotNull();

    // 1. Populate cache
    authService.getCurrentUser(studentId);
    assertThat(cache.get(studentId)).isNotNull();
    verify(studentRepository, times(1)).findById(studentId);

    // 2. Delete student (evicts cache)
    studentService.deleteStudent(studentId);

    // 3. Cache entry must be evicted
    assertThat(cache.get(studentId)).isNull();

    // 4. Next call to getCurrentUser attempts repository lookup again
    when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> authService.getCurrentUser(studentId));
    verify(studentRepository, times(3)).findById(studentId);
  }

  @Test
  @DisplayName(
      "Security Invariant: Profile cache mutation does not alter SecurityContext authorities")
  void cachedProfile_ResponsibilityChange_DoesNotAffectSecurityContextAuthorities() {
    // Simulate authenticated session with STUDENT role
    StudentPrincipal principal = StudentPrincipal.from(student);
    Authentication auth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

    assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");

    // Mutate responsibility in DB/cache
    studentService.updateResponsibility(studentId, Responsibility.CLASS_REPRESENTATIVE);

    // Invariant: SecurityContext authorities remain derived from authenticated principal
    assertThat(auth.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_STUDENT")
        .doesNotContain("ROLE_CLASS_REPRESENTATIVE");
  }
}
