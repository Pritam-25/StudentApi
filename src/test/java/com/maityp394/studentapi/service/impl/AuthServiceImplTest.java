package com.maityp394.studentapi.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.session.RedisSessionService;
import com.maityp394.studentapi.security.token.RefreshToken;
import com.maityp394.studentapi.security.token.TokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

  private RedisSessionService redisSessionService;
  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    StudentRepository studentRepository = mock(StudentRepository.class);
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
}
