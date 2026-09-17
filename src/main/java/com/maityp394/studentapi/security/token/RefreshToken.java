package com.maityp394.studentapi.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Structured opaque refresh token representation with cryptographic hashing and verification
 * helpers.
 *
 * <p>Format: {@code rt_<sessionId>_<secret>}
 *
 * @param sessionId the UUID of the session to which this token belongs
 * @param secret the cryptographically random secret component
 */
public record RefreshToken(UUID sessionId, String secret) {

  private static final String PREFIX = "rt_";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int SECRET_BYTES = 32;

  /**
   * Generates a new random {@link RefreshToken} for the given session ID.
   *
   * @param sessionId the session UUID
   * @return newly minted {@link RefreshToken}
   */
  public static RefreshToken create(UUID sessionId) {
    byte[] randomBytes = new byte[SECRET_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    return new RefreshToken(sessionId, secret);
  }

  /**
   * Serializes the refresh token into its structured string representation for client delivery.
   *
   * @return the serialized token string (e.g. {@code rt_<sessionId>_<secret>})
   */
  public String toTokenString() {
    return PREFIX + sessionId + "_" + secret;
  }

  /**
   * Computes the lowercase hex-encoded SHA-256 hash of this token's secret.
   *
   * @return hex-encoded SHA-256 digest
   */
  public String hash() {
    return hash(this.secret);
  }

  /**
   * Parses a raw refresh token string into a structured {@link RefreshToken}.
   *
   * @param rawToken the raw string received from cookie or header
   * @return parsed {@link RefreshToken}
   * @throws IllegalArgumentException if the format is invalid
   */
  public static RefreshToken parse(String rawToken) {
    if (rawToken == null || !rawToken.startsWith(PREFIX)) {
      throw new IllegalArgumentException("Invalid refresh token format: missing prefix");
    }
    String content = rawToken.substring(PREFIX.length());
    int separatorIndex = content.indexOf('_');
    if (separatorIndex <= 0 || separatorIndex == content.length() - 1) {
      throw new IllegalArgumentException("Invalid refresh token format: missing separator");
    }
    String sessionIdStr = content.substring(0, separatorIndex);
    String secret = content.substring(separatorIndex + 1);

    try {
      UUID sessionId = UUID.fromString(sessionIdStr);
      return new RefreshToken(sessionId, secret);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid refresh token format: invalid session UUID", e);
    }
  }

  /**
   * Computes the lowercase hex-encoded SHA-256 hash of the secret.
   *
   * @param secret raw secret string
   * @return hex-encoded SHA-256 digest
   */
  public static String hash(String secret) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Verifies in constant time if a candidate secret matches an expected hash.
   *
   * @param secret candidate secret string
   * @param expectedHash hex-encoded expected SHA-256 hash
   * @return true if matches, false otherwise
   */
  public static boolean matches(String secret, String expectedHash) {
    if (secret == null || expectedHash == null) {
      return false;
    }
    String candidateHash = hash(secret);
    return MessageDigest.isEqual(
        candidateHash.getBytes(StandardCharsets.UTF_8),
        expectedHash.getBytes(StandardCharsets.UTF_8));
  }
}
