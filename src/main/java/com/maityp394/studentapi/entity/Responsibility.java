package com.maityp394.studentapi.entity;

/** Represents the role or responsibility assigned to a student within the institution. */
public enum Responsibility {
  /** Standard student role with normal permissions. */
  STUDENT,

  /** Student acting as class representative with elevated representation duties. */
  CLASS_REPRESENTATIVE;

  /**
   * Returns the Spring Security authority format (e.g. {@code "ROLE_CLASS_REPRESENTATIVE"}).
   *
   * @return the authority string prefixed with {@code ROLE_}
   */
  public String toAuthority() {
    return "ROLE_" + name();
  }
}
