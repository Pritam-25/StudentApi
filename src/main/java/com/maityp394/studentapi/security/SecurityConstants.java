package com.maityp394.studentapi.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.WebUtils;

/** Utility constants and shared helper methods for security and authentication mechanisms. */
public final class SecurityConstants {

  /** Name of the HttpOnly access token cookie. */
  public static final String ACCESS_TOKEN_COOKIE = "access_token";

  /** Name of the HttpOnly refresh token cookie. */
  public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

  /** Standard prefix for Bearer authorization headers. */
  public static final String BEARER_PREFIX = "Bearer ";

  private SecurityConstants() {}

  /**
   * Checks whether the given request contains an {@code access_token} cookie with a non-blank
   * value.
   *
   * @param request the incoming HTTP request
   * @return {@code true} if a valid, non-blank access token cookie is present, otherwise {@code
   *     false}
   */
  public static boolean hasAccessTokenCookie(HttpServletRequest request) {
    return getAccessTokenFromCookie(request) != null;
  }

  /**
   * Extracts the access token string from the {@code access_token} cookie, if present.
   *
   * @param request the incoming HTTP request
   * @return the token string or {@code null} if absent or blank
   */
  public static String getAccessTokenFromCookie(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, ACCESS_TOKEN_COOKIE);
    return (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank())
        ? cookie.getValue()
        : null;
  }

  /**
   * Checks whether the given request contains a {@code refresh_token} cookie with a non-blank
   * value.
   *
   * @param request the incoming HTTP request
   * @return {@code true} if a valid, non-blank refresh token cookie is present, otherwise {@code
   *     false}
   */
  public static boolean hasRefreshTokenCookie(HttpServletRequest request) {
    return getRefreshTokenFromCookie(request) != null;
  }

  /**
   * Extracts the refresh token string from the {@code refresh_token} cookie, if present.
   *
   * @param request the incoming HTTP request
   * @return the token string or {@code null} if absent or blank
   */
  public static String getRefreshTokenFromCookie(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, REFRESH_TOKEN_COOKIE);
    return (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank())
        ? cookie.getValue()
        : null;
  }
}
