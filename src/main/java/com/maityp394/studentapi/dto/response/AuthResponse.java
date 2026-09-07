package com.maityp394.studentapi.dto.response;

/**
 * Authentication response containing the issued JWT bearer token.
 *
 * @param accessToken the signed JWT access token
 * @param tokenType the type of token, typically "Bearer"
 * @param expiresIn token validity duration in seconds
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
