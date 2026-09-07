package com.maityp394.studentapi.controller;

import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.ApiResponse;
import com.maityp394.studentapi.dto.response.AuthResponse;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.service.AuthService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** REST controller exposing authentication, registration, and user identity endpoints. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Registers a new student account using the standard student creation payload.
   *
   * @param request the registration request payload
   * @param ucb URI components builder to construct the location header
   * @return HTTP 201 Created with student details
   */
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<StudentResponse>> register(
      @Valid @RequestBody RegisterRequest request, UriComponentsBuilder ucb) {
    StudentResponse response = authService.register(request);
    URI location = ucb.path("/api/v1/students/{id}").buildAndExpand(response.getId()).toUri();
    return ResponseEntity.created(location)
        .body(new ApiResponse<>("Registration successful", response));
  }

  /**
   * Authenticates user credentials and issues a JWT access token.
   *
   * @param request the login credentials payload
   * @return HTTP 200 OK with the generated bearer token
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(new ApiResponse<>("Login successful", response));
  }

  /**
   * Retrieves profile information for the currently authenticated user.
   *
   * @param jwt the validated JWT principal injected by Spring Security
   * @return HTTP 200 OK with the student profile response
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<StudentResponse>> me(@AuthenticationPrincipal Jwt jwt) {
    StudentResponse response = authService.getCurrentUser(UUID.fromString(jwt.getSubject()));
    return ResponseEntity.ok(new ApiResponse<>("User fetched successfully", response));
  }

  /**
   * Logs out the user by clearing the security context.
   *
   * @return HTTP 200 OK confirming logout
   */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout() {
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(new ApiResponse<>("Logged out successfully", null));
  }
}
