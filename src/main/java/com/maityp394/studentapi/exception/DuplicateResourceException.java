package com.maityp394.studentapi.exception;

/** Exception thrown when a duplicate resource is detected. */
public class DuplicateResourceException extends ApplicationException {

  /**
   * Constructs a new {@code DuplicateResourceException} with the specified {@link ErrorCode}.
   *
   * @param errorCode the associated error code
   */
  public DuplicateResourceException(ErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * Constructs a new {@code DuplicateResourceException} with the specified {@link ErrorCode} and
   * custom message.
   *
   * @param errorCode the associated error code
   * @param message descriptive error message explaining which resource was duplicated
   */
  public DuplicateResourceException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
