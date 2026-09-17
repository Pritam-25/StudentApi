package com.maityp394.studentapi.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when an OAuth2 authentication flow encounters an existing account with the same
 * email address configured for local password authentication.
 */
public class OAuthAccountLinkingException extends AuthenticationException {

  public OAuthAccountLinkingException(String msg) {
    super(msg);
  }

  public OAuthAccountLinkingException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
