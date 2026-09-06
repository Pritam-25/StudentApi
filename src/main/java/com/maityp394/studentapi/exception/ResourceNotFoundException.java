package com.maityp394.studentapi.exception;

/** Exception thrown when a requested resource cannot be found in the system. */
public class ResourceNotFoundException extends ApplicationException {

  /**
   * Constructs a new {@code ResourceNotFoundException} with the specified {@link ErrorCode}.
   *
   * @param errorCode the associated error code
   */
  public ResourceNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * Constructs a new {@code ResourceNotFoundException} with the specified {@link ErrorCode} and
   * custom message.
   *
   * @param errorCode the associated error code
   * @param message descriptive error message explaining which resource was missing
   */
  public ResourceNotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
