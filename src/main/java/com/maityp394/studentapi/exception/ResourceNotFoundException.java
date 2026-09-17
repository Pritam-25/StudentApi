package com.maityp394.studentapi.exception;

/** Exception thrown when a requested resource cannot be found in the system. */
public class ResourceNotFoundException extends ApplicationException {

  public ResourceNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }

  public ResourceNotFoundException(ErrorCode errorCode, String detail) {
    super(errorCode, detail);
  }
}
