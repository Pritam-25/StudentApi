package com.maityp394.studentapi.service;

import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.AuthResponse;
import com.maityp394.studentapi.dto.response.StudentResponse;
import java.util.UUID;

/** Service interface defining authentication and user identity management operations. */
public interface AuthService {

  /**
   * Registers a new student account using the provided student creation payload.
   *
   * @param request the registration details
   * @return the registered student response
   */
  StudentResponse register(RegisterRequest request);

  /**
   * Authenticates student credentials and issues a signed JWT access token.
   *
   * @param request the login credentials
   * @return an {@link AuthResponse} containing the JWT bearer token
   */
  AuthResponse login(LoginRequest request);

  /**
   * Retrieves profile details for the authenticated student.
   *
   * @param studentId the UUID of the authenticated student
   * @return the student profile response
   */
  StudentResponse getCurrentUser(UUID studentId);
}
