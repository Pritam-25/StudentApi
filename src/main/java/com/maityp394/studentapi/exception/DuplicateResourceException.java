package com.maityp394.studentapi.exception;

/** Exception thrown when a duplicate resource is detected. */
public class DuplicateResourceException extends ApplicationException {

  public DuplicateResourceException(ErrorCode errorCode) {
    super(errorCode);
  }

  public DuplicateResourceException(ErrorCode errorCode, String detail) {
    super(errorCode, detail);
  }
}
