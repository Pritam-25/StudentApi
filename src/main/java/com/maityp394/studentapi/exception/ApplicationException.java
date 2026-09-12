package com.maityp394.studentapi.exception;

import lombok.Getter;

/** Base abstract application exception carrying a machine-readable {@link ErrorCode}. */
@Getter
public abstract class ApplicationException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detail;

  protected ApplicationException(ErrorCode errorCode) {
    super(errorCode.getTitle());
    this.errorCode = errorCode;
    this.detail = null;
  }

  protected ApplicationException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = errorCode;
    this.detail = detail;
  }
}
