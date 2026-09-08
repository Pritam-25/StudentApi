package com.maityp394.studentapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Standard business error codes representing application-specific error states. */
@Getter
public enum ErrorCode {

  // ============================================================
  // Common / Infrastructure / Validation
  // ============================================================
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation Failed"),
  INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "Invalid Parameter Type"),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid Request"),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource Not Found"),
  DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "Data Integrity Violation"),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),

  // ============================================================
  // Student Domain
  // ============================================================
  STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Student Not Found"),
  STUDENT_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Student Email Already Exists"),

  // ============================================================
  // Authentication & Authorization (Security)
  // ============================================================
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid Credentials"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Unauthorized"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
  CSRF_INVALID(HttpStatus.FORBIDDEN, "Forbidden");

  private final HttpStatus status;
  private final String title;

  ErrorCode(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }
}
