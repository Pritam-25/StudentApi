package com.maityp394.studentapi.exception;

import lombok.Getter;

/** Base abstract application exception carrying a machine-readable {@link ErrorCode}. */
@Getter
public abstract class ApplicationException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detail;

  /**
   * Constructs a new {@code ApplicationException} with stable title from the {@link ErrorCode} and
   * no occurrence-specific detail.
   *
   * @param errorCode the associated business error code
   */
  protected ApplicationException(ErrorCode errorCode) {
    super(errorCode.getTitle());
    this.errorCode = errorCode;
    this.detail = null;
  }

  /**
   * Constructs a new {@code ApplicationException} with a custom occurrence-specific detail message.
   *
   * @param errorCode the associated business error code
   * @param detail the occurrence-specific detail message
   */
  protected ApplicationException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = errorCode;
    this.detail = detail;
  }
}
