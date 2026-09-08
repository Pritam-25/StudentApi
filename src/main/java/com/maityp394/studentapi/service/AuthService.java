package com.maityp394.studentapi.service;

import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.AuthResult;
import com.maityp394.studentapi.dto.response.StudentResponse;
import java.util.UUID;

/** Service interface defining authentication and user identity management operations. */
public interface AuthService {

  /**
   * Registers a new student account, establishes an authenticated session, and returns an auth
   * result.
   *
   * @param request the registration details
   * @return an {@link AuthResult} containing student information and token metadata
   */
  AuthResult register(RegisterRequest request);

  /**
   * Authenticates student credentials and issues a signed JWT access token.
   *
   * @param request the login credentials
   * @return an {@link AuthResult} containing student information and token metadata
   */
  AuthResult login(LoginRequest request);

  /**
   * Retrieves profile details for the authenticated student.
   *
   * @param studentId the UUID of the authenticated student
   * @return the student profile response
   */
  StudentResponse getCurrentUser(UUID studentId);
}
