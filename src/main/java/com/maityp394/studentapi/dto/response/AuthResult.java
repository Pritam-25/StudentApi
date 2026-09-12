package com.maityp394.studentapi.dto.response;

import com.maityp394.studentapi.security.token.AuthTokens;

/**
 * Internal authentication result containing student profile information and token metadata.
 *
 * @param student the authenticated student profile details
 * @param tokens the issued authentication tokens and session metadata
 */
public record AuthResult(StudentResponse student, AuthTokens tokens) {}
