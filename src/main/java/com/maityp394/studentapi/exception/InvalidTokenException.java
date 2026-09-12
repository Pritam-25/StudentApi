package com.maityp394.studentapi.exception;

/** Exception thrown when a presented token or session is invalid, expired, or revoked. */
public class InvalidTokenException extends ApplicationException {

  public InvalidTokenException() {
    super(ErrorCode.INVALID_TOKEN);
  }

  public InvalidTokenException(String detail) {
    super(ErrorCode.INVALID_TOKEN, detail);
  }

  public InvalidTokenException(ErrorCode errorCode, String detail) {
    super(errorCode, detail);
  }
}
