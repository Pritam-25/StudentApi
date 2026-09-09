package com.maityp394.studentapi.dto.response;

/**
 * Internal authentication result containing student information and token metadata for cookie
 * generation.
 *
 * @param student the authenticated student profile details
 * @param accessToken the minted JWT access token string
 * @param expiresIn the token lifespan in seconds
 */
public record AuthResult(StudentResponse student, String accessToken, long expiresIn) {}
