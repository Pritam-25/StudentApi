package com.maityp394.studentapi.controller;

import com.maityp394.studentapi.config.OpenApiConfig;
import com.maityp394.studentapi.config.properties.SecurityProperties;
import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.ApiResponse;
import com.maityp394.studentapi.dto.response.AuthResult;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.InvalidTokenException;
import com.maityp394.studentapi.security.SecurityConstants;
import com.maityp394.studentapi.security.token.AuthTokens;
import com.maityp394.studentapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** REST controller exposing authentication, registration, refresh, and session logout endpoints. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and session endpoints")
public class AuthController {

  private final AuthService authService;
  private final SecurityProperties securityProperties;

  /**
   * Registers a new student account, establishes an authenticated session with HttpOnly
   * access_token and refresh_token cookies, and returns student details.
   *
   * @param request student registration payload
   * @param servletRequest HTTP request used to extract user-agent and IP address
   * @param ucb URI components builder to construct the created student location header
   * @return {@link ResponseEntity} containing the created {@link StudentResponse} and session
   *     cookies
   */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a new student account")
  public ResponseEntity<ApiResponse<StudentResponse>> register(
      @Valid @RequestBody RegisterRequest request,
      HttpServletRequest servletRequest,
      UriComponentsBuilder ucb) {
    ClientInfo client = extractClientInfo(servletRequest);
    AuthResult result = authService.register(request, client.userAgent(), client.ipAddress());
    URI location = ucb.path("/api/v1/students/{id}").buildAndExpand(result.student().id()).toUri();

    return ResponseEntity.created(location)
        .headers(buildCookieHeaders(result.tokens()))
        .body(new ApiResponse<>("Registration successful", result.student()));
  }

  /**
   * Authenticates user credentials, establishes an authenticated session with HttpOnly access_token
   * and refresh_token cookies, and returns student details.
   *
   * @param request user login credentials
   * @param servletRequest HTTP request used to extract user-agent and IP address
   * @return {@link ResponseEntity} containing authenticated {@link StudentResponse} and session
   *     cookies
   */
  @PostMapping("/login")
  @Operation(summary = "Authenticate student credentials")
  public ResponseEntity<ApiResponse<StudentResponse>> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    ClientInfo client = extractClientInfo(servletRequest);
    AuthResult result = authService.login(request, client.userAgent(), client.ipAddress());

    return ResponseEntity.ok()
        .headers(buildCookieHeaders(result.tokens()))
        .body(new ApiResponse<>("Login successful", result.student()));
  }

  /**
   * Rotates the refresh token and issues a new access token and rotated refresh token via HttpOnly
   * cookies.
   *
   * @param request HTTP request containing the refresh token cookie
   * @return {@link ResponseEntity} containing success status and renewed session cookies
   */
  @PostMapping("/refresh")
  @Operation(summary = "Rotate refresh token and issue new session tokens")
  @SecurityRequirement(name = OpenApiConfig.REFRESH_COOKIE_AUTH)
  public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request) {
    String rawRefreshToken = SecurityConstants.getRefreshTokenFromCookie(request);
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new InvalidTokenException(ErrorCode.UNAUTHORIZED, "Missing refresh token cookie");
    }

    ClientInfo client = extractClientInfo(request);
    AuthTokens tokens =
        authService.refresh(rawRefreshToken, client.userAgent(), client.ipAddress());

    return ResponseEntity.ok()
        .headers(buildCookieHeaders(tokens))
        .body(new ApiResponse<>("Token refreshed successfully", null));
  }

  /**
   * Retrieves profile information for the currently authenticated user.
   *
   * @param jwt authenticated JWT principal
   * @return {@link ResponseEntity} containing the current authenticated {@link StudentResponse}
   */
  @GetMapping("/me")
  @Operation(summary = "Get current authenticated student profile")
  public ResponseEntity<ApiResponse<StudentResponse>> me(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    StudentResponse response = authService.getCurrentUser(userId);
    return ResponseEntity.ok(new ApiResponse<>("User fetched successfully", response));
  }

  /**
   * Logs out the current device session by invalidating the session in Redis and clearing cookies.
   *
   * @param request HTTP request containing the refresh token cookie
   * @return {@link ResponseEntity} with cleared session cookies
   */
  @PostMapping("/logout")
  @Operation(summary = "Log out current device session and invalidate cookies")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
    String rawRefreshToken = SecurityConstants.getRefreshTokenFromCookie(request);
    authService.logout(rawRefreshToken);

    return ResponseEntity.ok()
        .headers(clearCookieHeaders())
        .body(new ApiResponse<>("Logged out successfully", null));
  }

  /**
   * Logs out all sessions for the authenticated user across all devices.
   *
   * @param jwt authenticated JWT principal
   * @return {@link ResponseEntity} with cleared session cookies
   */
  @PostMapping("/logout-all")
  @Operation(summary = "Log out all sessions across all devices for the current user")
  public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    authService.logoutAll(userId);

    return ResponseEntity.ok()
        .headers(clearCookieHeaders())
        .body(new ApiResponse<>("Logged out of all devices successfully", null));
  }

  private HttpHeaders buildCookieHeaders(AuthTokens tokens) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildAccessTokenCookie(tokens.accessToken(), tokens.accessTokenExpiresIn()).toString());
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildRefreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiresIn()).toString());
    return headers;
  }

  private HttpHeaders clearCookieHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, clearAccessTokenCookie().toString());
    headers.add(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString());
    return headers;
  }

  private ResponseCookie buildAccessTokenCookie(String token, long maxAge) {
    return ResponseCookie.from(SecurityConstants.ACCESS_TOKEN_COOKIE, token)
        .httpOnly(true)
        .secure(securityProperties.cookie().secure())
        .path("/")
        .maxAge(maxAge)
        .sameSite("Lax")
        .build();
  }

  private ResponseCookie buildRefreshTokenCookie(String token, long maxAge) {
    return ResponseCookie.from(SecurityConstants.REFRESH_TOKEN_COOKIE, token)
        .httpOnly(true)
        .secure(securityProperties.cookie().secure())
        .path("/api/v1/auth")
        .maxAge(maxAge)
        .sameSite("Lax")
        .build();
  }

  private ResponseCookie clearAccessTokenCookie() {
    return ResponseCookie.from(SecurityConstants.ACCESS_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(securityProperties.cookie().secure())
        .path("/")
        .maxAge(0)
        .sameSite("Lax")
        .build();
  }

  private ResponseCookie clearRefreshTokenCookie() {
    return ResponseCookie.from(SecurityConstants.REFRESH_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(securityProperties.cookie().secure())
        .path("/api/v1/auth")
        .maxAge(0)
        .sameSite("Lax")
        .build();
  }

  private record ClientInfo(String userAgent, String ipAddress) {}

  private ClientInfo extractClientInfo(HttpServletRequest request) {
    return new ClientInfo(request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
  }
}
