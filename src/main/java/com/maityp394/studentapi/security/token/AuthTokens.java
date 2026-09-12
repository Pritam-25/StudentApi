package com.maityp394.studentapi.security.token;

import java.util.UUID;

/**
 * Encapsulates the complete pair of issued authentication tokens and associated session metadata.
 *
 * @param accessToken short-lived JWT access token string
 * @param accessTokenExpiresIn access token validity in seconds
 * @param refreshToken long-lived rotating refresh token string
 * @param refreshTokenExpiresIn refresh token validity in seconds
 * @param sessionId unique UUID of the server-side Redis session
 */
public record AuthTokens(
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn,
    UUID sessionId) {}
