package com.maityp394.studentapi.security.session;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Server-side session record stored in Redis.
 *
 * <p>Tracks an active user login, its refresh token family, sliding inactivity expiration, and hard
 * absolute lifetime limit.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "refreshTokenHash")
public class UserSession {

  /** Unique identifier for this login session. */
  private UUID sessionId;

  /** Unique identifier of the authenticated student. */
  private UUID userId;

  /** Refresh token family identifier used to detect token replay and revoke family. */
  private UUID refreshFamilyId;

  /** Hex-encoded SHA-256 hash of the currently active refresh token secret. */
  private String refreshTokenHash;

  /** Current session status (ACTIVE or REVOKED). */
  private SessionStatus status;

  /** Timestamp when this session was first created. */
  private Instant createdAt;

  /** Timestamp of the most recent refresh or authenticated activity. */
  private Instant lastActivityAt;

  /** Absolute maximum expiration timestamp after which the session cannot be renewed. */
  private Instant absoluteExpiresAt;

  /** User-Agent header from the client creating or refreshing the session. */
  private String userAgent;

  /** Remote IP address of the client creating or refreshing the session. */
  private String ipAddress;

  /**
   * Returns true if the session is ACTIVE and has not passed its absolute expiration time.
   *
   * @param now the reference instant to validate expiration against
   * @return {@code true} if active and unexpired, {@code false} otherwise
   */
  public boolean isValid(Instant now) {
    return status == SessionStatus.ACTIVE && now.isBefore(absoluteExpiresAt);
  }
}
