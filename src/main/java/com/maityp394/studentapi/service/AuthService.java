package com.maityp394.studentapi.service;

import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.AuthResult;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.security.token.AuthTokens;
import java.util.UUID;

/**
 * Service interface defining authentication, token lifecycle, and user identity management
 * operations.
 */
public interface AuthService {

  /**
   * Registers a new student account, establishes an authenticated session, and returns an auth
   * result.
   *
   * @param request the registration details
   * @param userAgent client User-Agent header
   * @param ipAddress client IP address
   * @return an {@link AuthResult} containing student information and token metadata
   */
  AuthResult register(RegisterRequest request, String userAgent, String ipAddress);

  /**
   * Authenticates student credentials and issues application tokens with a Redis session.
   *
   * @param request the login credentials
   * @param userAgent client User-Agent header
   * @param ipAddress client IP address
   * @return an {@link AuthResult} containing student information and token metadata
   */
  AuthResult login(LoginRequest request, String userAgent, String ipAddress);

  /**
   * Atomically rotates a refresh token, validating session state and detecting token replay.
   *
   * @param rawRefreshToken the presented raw refresh token string
   * @param userAgent client User-Agent
   * @param ipAddress client IP address
   * @return fresh {@link AuthTokens}
   */
  AuthTokens refresh(String rawRefreshToken, String userAgent, String ipAddress);

  /**
   * Logs out the current device session by invalidating the session in Redis.
   *
   * @param rawRefreshToken raw refresh token identifying the session to revoke
   */
  void logout(String rawRefreshToken);

  /**
   * Logs out all sessions for the given student across all devices.
   *
   * @param userId UUID of the authenticated student
   */
  void logoutAll(UUID userId);

  /**
   * Retrieves profile details for the authenticated student.
   *
   * @param studentId the UUID of the authenticated student
   * @return the student profile response
   */
  StudentResponse getCurrentUser(UUID studentId);
}
