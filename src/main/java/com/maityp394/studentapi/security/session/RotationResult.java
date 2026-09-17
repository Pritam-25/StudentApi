package com.maityp394.studentapi.security.session;

/** Result status of an atomic refresh token rotation attempt against Redis. */
public enum RotationResult {

  /** Token rotated successfully; new hash and activity timestamp recorded. */
  SUCCESS,

  /**
   * Stored token hash did not match expected hash, indicating token reuse or theft. The session
   * status has been transitioned to REVOKE.
   */
  REUSE_DETECTED,

  /** Session key was not found in Redis or the session was already inactive/revoked. */
  SESSION_INVALID_OR_NOT_FOUND
}
