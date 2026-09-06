package com.maityp394.studentapi.dto.response;

import java.time.Instant;
import lombok.Getter;

/**
 * Generic standard API response envelope for unifying successful (2xx) REST endpoint responses.
 *
 * @param <T> the type of the payload data
 */
@Getter
public class ApiResponse<T> {

  private final String message;
  private final T data;
  private final Instant timestamp;

  /**
   * Constructs a new {@code ApiResponse} instance with the given message and data, setting the
   * timestamp to the current UTC instant.
   *
   * @param message human-readable status or descriptive message
   * @param data payload data returned by the operation
   */
  public ApiResponse(String message, T data) {
    this.message = message;
    this.data = data;
    this.timestamp = Instant.now();
  }
}
