package com.maityp394.studentapi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.OAuthAccountLinkingException;
import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.session.RedisSessionService;
import com.maityp394.studentapi.security.token.RefreshToken;
import com.maityp394.studentapi.security.token.TokenService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

  private StudentRepository studentRepository;
  private RedisSessionService redisSessionService;
  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    studentRepository = mock(StudentRepository.class);
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    StudentMapper studentMapper = mock(StudentMapper.class);
    TokenService tokenService = mock(TokenService.class);
    redisSessionService = mock(RedisSessionService.class);

    authService =
        new AuthServiceImpl(
            studentRepository,
            authenticationManager,
            passwordEncoder,
            studentMapper,
            tokenService,
            redisSessionService);
  }

  @Test
  @DisplayName("logout should catch IllegalArgumentException for malformed refresh tokens")
  void shouldCatchIllegalArgumentExceptionOnMalformedToken() {
    assertThatCode(() -> authService.logout("malformed_refresh_token")).doesNotThrowAnyException();
    verify(redisSessionService, never()).revokeSession(any());
  }

  @Test
  @DisplayName("logout should return immediately for blank or null refresh token")
  void shouldDoNothingOnBlankToken() {
    authService.logout(null);
    authService.logout("   ");
    verify(redisSessionService, never()).revokeSession(any());
  }

  @Test
  @DisplayName("logout should successfully revoke session for valid refresh token")
  void shouldRevokeSessionOnValidToken() {
    UUID sessionId = UUID.randomUUID();
    RefreshToken token = RefreshToken.create(sessionId);

    authService.logout(token.toTokenString());

    verify(redisSessionService).revokeSession(sessionId);
  }

  @Test
  @DisplayName("logout should allow Redis revocation failures to propagate")
  void shouldPropagateRedisFailureDuringLogout() {
    UUID sessionId = UUID.randomUUID();
    RefreshToken token = RefreshToken.create(sessionId);

    doThrow(new QueryTimeoutException("Redis timeout"))
        .when(redisSessionService)
        .revokeSession(sessionId);

    assertThatThrownBy(() -> authService.logout(token.toTokenString()))
        .isInstanceOf(QueryTimeoutException.class)
        .hasMessageContaining("Redis timeout");
  }

  @Test
  @DisplayName("processGoogleUser should return existing student when found by Google Subject")
  void shouldReturnExistingStudentWhenFoundByGoogleSubject() {
    Student existing =
        new Student("Pritam", "pritam@example.com", Responsibility.STUDENT, "google-sub-123");
    when(studentRepository.findByGoogleSubject("google-sub-123")).thenReturn(Optional.of(existing));

    Student result =
        authService.processGoogleUser("google-sub-123", "pritam@example.com", "Pritam");

    assertThat(result).isEqualTo(existing);
    verify(studentRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "processGoogleUser should throw OAuthAccountLinkingException when email exists with local password")
  void shouldThrowAccountLinkingExceptionWhenEmailExistsWithPassword() {
    Student existing =
        new Student("Pritam", "pritam@example.com", "hashed-pass", Responsibility.STUDENT);
    when(studentRepository.findByGoogleSubject("google-sub-456")).thenReturn(Optional.empty());
    when(studentRepository.findByEmail("pritam@example.com")).thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () -> authService.processGoogleUser("google-sub-456", "pritam@example.com", "Pritam"))
        .isInstanceOf(OAuthAccountLinkingException.class)
        .hasMessageContaining("An account with this email already exists");
  }

  @Test
  @DisplayName("processGoogleUser should link Google account when email exists without password")
  void shouldLinkGoogleAccountWhenEmailExistsWithoutPassword() {
    Student existing = new Student("Pritam", "pritam@example.com", null, Responsibility.STUDENT);
    when(studentRepository.findByGoogleSubject("google-sub-789")).thenReturn(Optional.empty());
    when(studentRepository.findByEmail("pritam@example.com")).thenReturn(Optional.of(existing));
    when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

    Student result =
        authService.processGoogleUser("google-sub-789", "pritam@example.com", "Pritam");

    assertThat(result.isGoogleLinked()).isTrue();
    assertThat(result.getGoogleSubject()).isEqualTo("google-sub-789");
    verify(studentRepository).save(existing);
  }

  @Test
  @DisplayName(
      "processGoogleUser should provision new student when neither subject nor email exists")
  void shouldProvisionNewStudentWhenNeitherSubjectNorEmailExists() {
    when(studentRepository.findByGoogleSubject("google-sub-new")).thenReturn(Optional.empty());
    when(studentRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

    Student result = authService.processGoogleUser("google-sub-new", "new@example.com", "New User");

    assertThat(result.getName()).isEqualTo("New User");
    assertThat(result.getEmail()).isEqualTo("new@example.com");
    assertThat(result.getResponsibility()).isEqualTo(Responsibility.STUDENT);
    assertThat(result.getGoogleSubject()).isEqualTo("google-sub-new");
    verify(studentRepository).save(any(Student.class));
  }
}
