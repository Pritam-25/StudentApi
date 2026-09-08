package com.maityp394.studentapi.exception;

/**
 * Exception thrown when an authenticated user attempts an operation they are not authorized to
 * perform (e.g., modifying another user's resource).
 */
public class ForbiddenException extends ApplicationException {

  public ForbiddenException() {
    super(ErrorCode.FORBIDDEN);
  }

  public ForbiddenException(String detail) {
    super(ErrorCode.FORBIDDEN, detail);
  }
}
