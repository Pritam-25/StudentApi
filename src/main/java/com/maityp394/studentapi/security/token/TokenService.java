package com.maityp394.studentapi.security.token;

import com.maityp394.studentapi.config.properties.RedisSessionProperties;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.InvalidTokenException;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.session.RedisSessionService;
import com.maityp394.studentapi.security.session.RotationResult;
import com.maityp394.studentapi.security.session.UserSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service managing token lifecycle: initial minting, atomic rotation, and token replay/theft
 * detection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

  private final JwtService jwtService;
  private final RedisSessionService redisSessionService;
  private final RedisSessionProperties sessionProperties;
  private final StudentRepository studentRepository;

  /**
   * Issues a new access token, refresh token, and Redis session for an authenticated student.
   *
   * @param student the authenticated student
   * @param userAgent client User-Agent
   * @param ipAddress client IP address
   * @return complete {@link AuthTokens} containing access and refresh tokens
   */
  public AuthTokens issueTokens(Student student, String userAgent, String ipAddress) {
    UUID sessionId = UUID.randomUUID();
    RefreshToken refreshToken = RefreshToken.create(sessionId);
    String initialHash = refreshToken.hash();

    redisSessionService.createSession(
        sessionId, student.getId(), initialHash, userAgent, ipAddress);

    String accessToken = jwtService.generateAccessToken(student, sessionId);

    return new AuthTokens(
        accessToken,
        jwtService.getExpirationSeconds(),
        refreshToken.toTokenString(),
        sessionProperties.idleTimeoutSeconds(),
        sessionId);
  }

  /**
   * Atomically rotates a refresh token, validating session state and detecting token replay.
   *
   * @param rawRefreshToken the presented raw refresh token string
   * @param userAgent client User-Agent
   * @param ipAddress client IP address
   * @return new {@link AuthTokens} with rotated refresh token and fresh access token
   */
  public AuthTokens rotateTokens(String rawRefreshToken, String userAgent, String ipAddress) {
    RefreshToken currentToken;
    try {
      currentToken = RefreshToken.parse(rawRefreshToken);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid refresh token structure received: {}", e.getMessage());
      throw new InvalidTokenException(ErrorCode.INVALID_TOKEN, "Invalid refresh token format");
    }

    String expectedOldHash = currentToken.hash();
    Optional<UserSession> sessionOpt = redisSessionService.getSession(currentToken.sessionId());

    if (sessionOpt.isEmpty()) {
      log.warn("Refresh failed: session {} not found in Redis", currentToken.sessionId());
      throw new InvalidTokenException(ErrorCode.INVALID_TOKEN, "Session expired or invalid");
    }

    UserSession session = sessionOpt.get();
    Instant now = Instant.now();

    if (!session.isValid(now)) {
      log.warn("Refresh failed: session {} is inactive or expired", currentToken.sessionId());
      redisSessionService.revokeSession(currentToken.sessionId());
      throw new InvalidTokenException(ErrorCode.INVALID_TOKEN, "Session has expired");
    }

    // Generate the next refresh token secret
    RefreshToken nextToken = RefreshToken.create(currentToken.sessionId());
    String newHash = nextToken.hash();

    RotationResult rotationResult =
        redisSessionService.rotateRefreshToken(
            currentToken.sessionId(), expectedOldHash, newHash, userAgent, ipAddress);

    if (rotationResult == RotationResult.REUSE_DETECTED) {
      // Replay detected!
      log.error(
          "SECURITY ALERT: Refresh token reuse detected for session {} and user {}! Revoking session family.",
          currentToken.sessionId(),
          session.getUserId());
      redisSessionService.revokeSession(currentToken.sessionId());
      throw new InvalidTokenException(
          ErrorCode.REFRESH_TOKEN_REUSE_DETECTED,
          "Refresh token reuse detected. Your session has been revoked for security.");
    }

    if (rotationResult == RotationResult.SESSION_INVALID_OR_NOT_FOUND) {
      throw new InvalidTokenException(ErrorCode.INVALID_TOKEN, "Session state invalid or revoked");
    }

    // Load student to mint new JWT access token
    Student student =
        studentRepository
            .findById(session.getUserId())
            .orElseThrow(
                () ->
                    new InvalidTokenException(
                        ErrorCode.UNAUTHORIZED, "User account no longer exists"));

    String accessToken = jwtService.generateAccessToken(student, session.getSessionId());

    return new AuthTokens(
        accessToken,
        jwtService.getExpirationSeconds(),
        nextToken.toTokenString(),
        sessionProperties.idleTimeoutSeconds(),
        session.getSessionId());
  }
}
