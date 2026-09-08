package com.maityp394.studentapi.dto.response;

import java.time.Instant;

/**
 * Generic standard API response envelope for unifying successful (2xx) REST endpoint responses.
 *
 * @param message human-readable status or descriptive message
 * @param data payload data returned by the operation
 * @param timestamp UTC timestamp when the response envelope was created
 * @param <T> the type of the payload data
 */
public record ApiResponse<T>(String message, T data, Instant timestamp) {

  /**
   * Constructs a new {@code ApiResponse} instance, automatically setting the timestamp to the
   * current UTC instant.
   *
   * @param message human-readable status or descriptive message
   * @param data payload data returned by the operation
   */
  public ApiResponse(String message, T data) {
    this(message, data, Instant.now());
  }
}
