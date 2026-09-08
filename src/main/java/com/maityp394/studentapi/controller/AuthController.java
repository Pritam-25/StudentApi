package com.maityp394.studentapi.controller;

import com.maityp394.studentapi.config.properties.SecurityProperties;
import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.ApiResponse;
import com.maityp394.studentapi.dto.response.AuthResult;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.service.AuthService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  private final SecurityProperties securityProperties;

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
   * Authenticates user credentials, sets an HttpOnly access_token cookie, and returns student
   * details.
   *
   * @param request the login credentials payload
   * @return HTTP 200 OK with student details and Set-Cookie header
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<StudentResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    AuthResult result = authService.login(request);

    ResponseCookie cookie =
        ResponseCookie.from("access_token", result.accessToken())
            .httpOnly(true)
            .secure(securityProperties.cookie().secure())
            .path("/")
            .maxAge(result.expiresIn())
            .sameSite("Lax")
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new ApiResponse<>("Login successful", result.student()));
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
   * Logs out the user by clearing the security context and clearing the access_token cookie.
   *
   * @return HTTP 200 OK confirming logout with expired cookie
   */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout() {

    ResponseCookie cleanupCookie =
        ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .secure(securityProperties.cookie().secure())
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cleanupCookie.toString())
        .body(new ApiResponse<>("Logged out successfully", null));
  }
}
