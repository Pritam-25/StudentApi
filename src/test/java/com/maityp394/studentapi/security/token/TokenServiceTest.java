package com.maityp394.studentapi.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.config.properties.RedisSessionProperties;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.InvalidTokenException;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.session.RedisSessionService;
import com.maityp394.studentapi.security.session.RotationResult;
import com.maityp394.studentapi.security.session.SessionStatus;
import com.maityp394.studentapi.security.session.UserSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenService Unit Tests")
class TokenServiceTest {

  private JwtService jwtService;
  private RedisSessionService redisSessionService;
  private RedisSessionProperties sessionProperties;
  private StudentRepository studentRepository;
  private TokenService tokenService;

  @BeforeEach
  void setUp() {
    jwtService = mock(JwtService.class);
    redisSessionService = mock(RedisSessionService.class);
    sessionProperties = new RedisSessionProperties(604800L, 2592000L);
    studentRepository = mock(StudentRepository.class);

    tokenService =
        new TokenService(jwtService, redisSessionService, sessionProperties, studentRepository);
  }

  @Test
  @DisplayName("issueTokens should generate valid tokens and persist Redis session")
  void shouldIssueTokens() {
    UUID studentId = UUID.randomUUID();
    Student student = new Student("Pritam", "pritam@example.com", "hash", Responsibility.STUDENT);
    student.setId(studentId);

    when(jwtService.generateAccessToken(eq(student), any(UUID.class)))
        .thenReturn("jwt.access.token");
    when(jwtService.getExpirationSeconds()).thenReturn(900L);

    AuthTokens tokens = tokenService.issueTokens(student, "Mozilla/5.0", "127.0.0.1");

    assertThat(tokens.accessToken()).isEqualTo("jwt.access.token");
    assertThat(tokens.accessTokenExpiresIn()).isEqualTo(900L);
    assertThat(tokens.refreshToken()).startsWith("rt_");
    assertThat(tokens.refreshTokenExpiresIn()).isEqualTo(604800L);
    assertThat(tokens.sessionId()).isNotNull();

    verify(redisSessionService)
        .createSession(
            eq(tokens.sessionId()), eq(studentId), anyString(), eq("Mozilla/5.0"), eq("127.0.0.1"));
  }

  @Test
  @DisplayName("rotateTokens should succeed and return fresh tokens on valid refresh")
  void shouldRotateTokensSuccessfully() {
    UUID sessionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    RefreshToken currentToken = RefreshToken.create(sessionId);
    String rawToken = currentToken.toTokenString();
    String currentHash = currentToken.hash();

    UserSession activeSession =
        UserSession.builder()
            .sessionId(sessionId)
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .refreshTokenHash(currentHash)
            .absoluteExpiresAt(Instant.now().plusSeconds(10000))
            .build();

    Student student = new Student("Pritam", "pritam@example.com", "hash", Responsibility.STUDENT);
    student.setId(userId);

    when(redisSessionService.getSession(sessionId)).thenReturn(Optional.of(activeSession));
    when(redisSessionService.rotateRefreshToken(
            eq(sessionId), eq(currentHash), anyString(), anyString(), anyString()))
        .thenReturn(RotationResult.SUCCESS);
    when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
    when(jwtService.generateAccessToken(student, sessionId)).thenReturn("jwt.fresh.access.token");
    when(jwtService.getExpirationSeconds()).thenReturn(900L);

    AuthTokens rotated = tokenService.rotateTokens(rawToken, "Chrome", "192.168.1.1");

    assertThat(rotated.accessToken()).isEqualTo("jwt.fresh.access.token");
    assertThat(rotated.refreshToken()).isNotEqualTo(rawToken);
    assertThat(rotated.sessionId()).isEqualTo(sessionId);
  }

  @Test
  @DisplayName("rotateTokens should detect token replay and revoke session when hash mismatches")
  void shouldDetectTokenReplayAndRevokeSession() {
    UUID sessionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    RefreshToken oldStolenToken = RefreshToken.create(sessionId);
    String rawToken = oldStolenToken.toTokenString();
    String oldHash = oldStolenToken.hash();

    UserSession activeSession =
        UserSession.builder()
            .sessionId(sessionId)
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .refreshTokenHash("already-rotated-hash")
            .absoluteExpiresAt(Instant.now().plusSeconds(10000))
            .build();

    when(redisSessionService.getSession(sessionId)).thenReturn(Optional.of(activeSession));
    when(redisSessionService.rotateRefreshToken(
            eq(sessionId), eq(oldHash), anyString(), anyString(), anyString()))
        .thenReturn(RotationResult.REUSE_DETECTED);

    assertThatThrownBy(() -> tokenService.rotateTokens(rawToken, "AttackerBrowser", "10.0.0.1"))
        .isInstanceOf(InvalidTokenException.class)
        .satisfies(
            ex -> {
              InvalidTokenException ite = (InvalidTokenException) ex;
              assertThat(ite.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
            });

    verify(redisSessionService).revokeSession(sessionId);
  }

  @Test
  @DisplayName(
      "rotateTokens should throw InvalidTokenException when session is expired past absolute limit")
  void shouldRejectExpiredSession() {
    UUID sessionId = UUID.randomUUID();
    RefreshToken token = RefreshToken.create(sessionId);
    String rawToken = token.toTokenString();

    UserSession expiredSession =
        UserSession.builder()
            .sessionId(sessionId)
            .userId(UUID.randomUUID())
            .status(SessionStatus.ACTIVE)
            .refreshTokenHash(token.hash())
            .absoluteExpiresAt(Instant.now().minusSeconds(10)) // expired in the past
            .build();

    when(redisSessionService.getSession(sessionId)).thenReturn(Optional.of(expiredSession));

    assertThatThrownBy(() -> tokenService.rotateTokens(rawToken, "Browser", "127.0.0.1"))
        .isInstanceOf(InvalidTokenException.class);

    verify(redisSessionService).revokeSession(sessionId);
  }
}
