package com.maityp394.studentapi.security.session;

import com.maityp394.studentapi.config.properties.RedisSessionProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Service managing Redis-backed user sessions and refresh token rotation state. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSessionService {

  private static final String SESSION_KEY_PREFIX = "session:";
  private static final String USER_SESSIONS_PREFIX = "user_sessions:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final RedisSessionProperties sessionProperties;
  private final RedisScript<Long> rotateRefreshTokenScript;

  /**
   * Creates and stores a new user session in Redis with an auto-generated session ID.
   *
   * @param userId UUID of the authenticated student
   * @param refreshTokenHash hex-encoded SHA-256 hash of the initial refresh token
   * @param userAgent client User-Agent header value
   * @param ipAddress client IP address
   * @return newly created {@link UserSession}
   */
  public UserSession createSession(
      UUID userId, String refreshTokenHash, String userAgent, String ipAddress) {
    return createSession(UUID.randomUUID(), userId, refreshTokenHash, userAgent, ipAddress);
  }

  /**
   * Creates and stores a new user session with a specified sessionId.
   *
   * @param sessionId specified UUID for the session
   * @param userId UUID of the authenticated student
   * @param refreshTokenHash hex-encoded SHA-256 hash of the initial refresh token
   * @param userAgent client User-Agent header value
   * @param ipAddress client IP address
   * @return newly created {@link UserSession}
   */
  public UserSession createSession(
      UUID sessionId, UUID userId, String refreshTokenHash, String userAgent, String ipAddress) {
    Instant now = Instant.now();
    UUID familyId = UUID.randomUUID();
    Instant absoluteExpiresAt = now.plusSeconds(sessionProperties.absoluteLifetimeSeconds());

    UserSession session =
        UserSession.builder()
            .sessionId(sessionId)
            .userId(userId)
            .refreshFamilyId(familyId)
            .refreshTokenHash(refreshTokenHash)
            .status(SessionStatus.ACTIVE)
            .createdAt(now)
            .lastActivityAt(now)
            .absoluteExpiresAt(absoluteExpiresAt)
            .userAgent(userAgent != null ? userAgent : "")
            .ipAddress(ipAddress != null ? ipAddress : "")
            .build();

    long ttlSeconds = calculateTtlSeconds(now, absoluteExpiresAt);
    saveSession(session, Duration.ofSeconds(ttlSeconds));

    redisTemplate.opsForSet().add(USER_SESSIONS_PREFIX + userId, sessionId.toString());
    log.debug("Created session {} for user {}", sessionId, userId);
    return session;
  }

  /**
   * Retrieves an active session by its unique session ID.
   *
   * @param sessionId session UUID
   * @return optional containing the {@link UserSession} if found and readable
   */
  public Optional<UserSession> getSession(UUID sessionId) {
    String key = SESSION_KEY_PREFIX + sessionId;
    String json = redisTemplate.opsForValue().get(key);
    if (json == null) {
      return Optional.empty();
    }
    try {
      UserSession session = objectMapper.readValue(json, UserSession.class);
      return Optional.of(session);
    } catch (Exception e) {
      log.error("Failed to deserialize session {}", sessionId, e);
      return Optional.empty();
    }
  }

  /**
   * Performs atomic compare-and-swap refresh token rotation in Redis using a Lua script.
   *
   * @param sessionId session UUID
   * @param expectedOldHash the hash of the presented refresh token
   * @param newHash the hash of the new refresh token
   * @param userAgent client User-Agent
   * @param ipAddress client IP address
   * @return {@link RotationResult} indicating success, token reuse, or invalid session
   */
  public RotationResult rotateRefreshToken(
      UUID sessionId, String expectedOldHash, String newHash, String userAgent, String ipAddress) {
    String key = SESSION_KEY_PREFIX + sessionId;
    Instant now = Instant.now();

    // Determine remaining TTL capped at absolute expiration
    Optional<UserSession> existingOpt = getSession(sessionId);
    if (existingOpt.isEmpty()) {
      return RotationResult.SESSION_INVALID_OR_NOT_FOUND;
    }
    UserSession existing = existingOpt.get();
    if (!existing.isValid(now)) {
      return RotationResult.SESSION_INVALID_OR_NOT_FOUND;
    }

    long ttlSeconds = calculateTtlSeconds(now, existing.getAbsoluteExpiresAt());

    Long result =
        redisTemplate.execute(
            rotateRefreshTokenScript,
            Collections.singletonList(key),
            expectedOldHash,
            newHash,
            now.toString(),
            userAgent != null ? userAgent : "",
            ipAddress != null ? ipAddress : "",
            String.valueOf(ttlSeconds));

    if (result == null || result == -1L) {
      return RotationResult.SESSION_INVALID_OR_NOT_FOUND;
    }
    if (result == 0L) {
      return RotationResult.REUSE_DETECTED;
    }
    return RotationResult.SUCCESS;
  }

  /**
   * Revokes and removes a single session from Redis and from the user's active session set.
   *
   * @param sessionId UUID of the session to revoke
   */
  public void revokeSession(UUID sessionId) {
    Optional<UserSession> sessionOpt = getSession(sessionId);
    String key = SESSION_KEY_PREFIX + sessionId;
    redisTemplate.delete(key);

    sessionOpt.ifPresent(
        session ->
            redisTemplate
                .opsForSet()
                .remove(USER_SESSIONS_PREFIX + session.getUserId(), sessionId.toString()));
    log.info("Revoked session {}", sessionId);
  }

  /**
   * Revokes all active sessions for the specified user (logout from all devices).
   *
   * @param userId UUID of the user whose sessions should be revoked
   */
  public void revokeAllUserSessions(UUID userId) {
    String userKey = USER_SESSIONS_PREFIX + userId;
    Set<String> sessionIds = redisTemplate.opsForSet().members(userKey);
    if (sessionIds != null && !sessionIds.isEmpty()) {
      List<String> keys = sessionIds.stream().map(id -> SESSION_KEY_PREFIX + id).toList();
      redisTemplate.delete(keys);
    }
    redisTemplate.delete(userKey);
    log.info("Revoked all sessions for user {}", userId);
  }

  private void saveSession(UserSession session, Duration ttl) {
    try {
      String json = objectMapper.writeValueAsString(session);
      redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + session.getSessionId(), json, ttl);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize UserSession", e);
    }
  }

  private long calculateTtlSeconds(Instant now, Instant absoluteExpiresAt) {
    long idleSeconds = sessionProperties.idleTimeoutSeconds();
    long secondsUntilAbsoluteExpiry =
        Math.max(1, Duration.between(now, absoluteExpiresAt).getSeconds());
    return Math.min(idleSeconds, secondsUntilAbsoluteExpiry);
  }
}
